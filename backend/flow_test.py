# -*- coding: utf-8 -*-
"""停车场业务全链路实测脚本 v2（UTF-8 安全）"""
import json, time, random, urllib.request, urllib.error, sys, subprocess

BASE = "http://localhost:8080/api/v1"
results = []

def call(method, path, body=None, token=None):
    req = urllib.request.Request(BASE + path, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    data = json.dumps(body).encode("utf-8") if body is not None else None
    try:
        with urllib.request.urlopen(req, data=data, timeout=20) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode("utf-8"))
        except Exception:
            return e.code, {}

def step(name, ok, detail=""):
    results.append("%s %s %s" % ("PASS" if ok else "FAIL", name, "" if ok else str(detail)[:200]))

suffix = str(int(time.time()))[-6:]
PLATE = "粤BF" + suffix[-4:] + random.choice("ACDEFGHJK")

s, r = call("POST", "/auth/register", {"username": "flow" + suffix, "password": "Password@123", "name": "流程测试", "phone": "138%08d" % (int(suffix) * 7 % 100000000)})
step("注册", r.get("code") == 200, r)
s, r = call("POST", "/auth/login", {"username": "flow" + suffix, "password": "Password@123"})
step("登录返回双令牌", r.get("code") == 200 and r["data"].get("token") and r["data"].get("refreshToken"), r)
access, refresh = r["data"]["token"], r["data"]["refreshToken"]

s, r = call("POST", "/auth/refresh", {"token": refresh})
step("refresh 轮换出新令牌", r.get("code") == 200 and r["data"]["refreshToken"] != refresh, r)
old_refresh = refresh
s, r = call("POST", "/auth/refresh", {"token": old_refresh})
step("旧 refresh 复用被拒(401)", r.get("code") == 401, r)

s, r = call("POST", "/vehicles", {"plateNumber": PLATE, "vehicleType": "SMALL"}, token=access)
step("新增车辆 " + PLATE, r.get("code") == 200, r)
vehicle_id = r["data"]["vehicleId"]

s, r = call("POST", "/auth/login", {"username": "admin", "password": "Password@123"})
admin_token = r["data"]["token"]
s, r = call("GET", "/parking-spaces?pageNum=1&pageSize=100", token=admin_token)
spaces = r["data"]["records"]
free = [x for x in spaces if x["status"] == "FREE"]
step("存在空闲车位", len(free) >= 2, "free=%d" % len(free))
space1, space2 = free[0], free[1]

# 预约 → 状态联动
s, r = call("POST", "/reservations", {"vehicleId": vehicle_id, "spaceId": space1["spaceId"],
            "arriveTime": time.strftime("%Y-%m-%dT%H:%M:%S", time.localtime(time.time() + 3600))}, token=access)
step("创建预约", r.get("code") == 200, r)
reservation_id = r["data"]["reservationId"]
s, r = call("GET", "/parking-spaces/%s/status" % space1["spaceId"], token=access)
step("预约后车位 RESERVED（缓存读回正常）", r["data"]["status"] == "RESERVED", r)
s, r = call("POST", "/reservations", {"vehicleId": vehicle_id, "spaceId": space2["spaceId"],
            "arriveTime": time.strftime("%Y-%m-%dT%H:%M:%S", time.localtime(time.time() + 7200))}, token=access)
step("已有 ACTIVE 预约时再次预约被拒(409)", r.get("code") == 409, r)

# 入场（走预约）→ 状态联动
s, r = call("POST", "/admin/parking-records/entry", {"plateNumber": PLATE, "spaceId": space1["spaceId"]}, token=admin_token)
step("登记入场", r.get("code") == 200, r)
record_id = r["data"]["recordId"]
s, r = call("GET", "/parking-spaces/%s/status" % space1["spaceId"], token=access)
step("入场后车位 OCCUPIED", r["data"]["status"] == "OCCUPIED", r)
s, r = call("GET", "/reservations/%s" % reservation_id, token=access)
step("入场后预约 USED", r["data"]["status"] == "USED", r)
s, r = call("POST", "/admin/parking-records/entry", {"plateNumber": PLATE, "spaceId": space1["spaceId"]}, token=admin_token)
step("重复入场被拒(409)", r.get("code") == 409, r)

# 占用中删除应被拒
s, r = call("DELETE", "/admin/parking-spaces/%s" % space1["spaceId"], token=admin_token)
step("占用中车位删除被拒(409)", r.get("code") == 409, r)
s, r = call("DELETE", "/vehicles/%s" % vehicle_id, token=access)
step("停车中车辆删除被拒(409)", r.get("code") == 409, r)

# 出场计费：入场时间回拨 3 小时 → 180 分钟，免费 15 → ceil(165/60)=3h → 15 元
subprocess.run(["docker", "exec", "smart-parking-mysql", "sh", "-c",
    'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" smart_parking -e "UPDATE parking_records SET entry_time=DATE_SUB(NOW(), INTERVAL 180 MINUTE) WHERE record_id=%s;"' % record_id],
    capture_output=True)
s, r = call("POST", "/admin/parking-records/exit", {"plateNumber": PLATE}, token=admin_token)
fee = r.get("data", {}).get("fee")
duration = r.get("data", {}).get("duration")
step("登记出场", r.get("code") == 200, r)
step("费用 15.00（3h、免费15分钟、5元/时）", str(fee) in ("15.00", "15.0"), "fee=%s duration=%s" % (fee, duration))
step("时长 180 分钟", duration == 180, duration)
s, r = call("GET", "/parking-spaces/%s/status" % space1["spaceId"], token=access)
step("出场后车位释放 FREE", r["data"]["status"] == "FREE", r)

# 过期闭环：space2 创建预约 → DB 回拨 arrive_time → 等定时任务
s, r = call("POST", "/reservations", {"vehicleId": vehicle_id, "spaceId": space2["spaceId"],
            "arriveTime": time.strftime("%Y-%m-%dT%H:%M:%S", time.localtime(time.time() + 7200))}, token=access)
step("创建待过期预约", r.get("code") == 200, r)
res2 = r["data"]["reservationId"]
subprocess.run(["docker", "exec", "smart-parking-mysql", "sh", "-c",
    'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" smart_parking -e "UPDATE reservations SET arrive_time=DATE_SUB(NOW(), INTERVAL 2 HOUR) WHERE reservation_id=%s;"' % res2],
    capture_output=True)
print("等待定时任务扫描（约 95 秒）...", flush=True)
time.sleep(95)

s, r = call("GET", "/reservations/%s" % res2, token=access)
step("定时任务推进预约 EXPIRED", r["data"]["status"] == "EXPIRED", r)
s, r = call("GET", "/parking-spaces/%s/status" % space2["spaceId"], token=access)
step("过期后车位释放 FREE", r["data"]["status"] == "FREE", r)
s, r = call("POST", "/reservations", {"vehicleId": vehicle_id, "spaceId": space2["spaceId"],
            "arriveTime": time.strftime("%Y-%m-%dT%H:%M:%S", time.localtime(time.time() + 7200))}, token=access)
step("过期后可再次预约（业务闭环）", r.get("code") == 200, r)
call("PUT", "/reservations/%s/cancel" % r["data"]["reservationId"], token=access)

# Dashboard
s, r = call("GET", "/admin/dashboard/statistics", token=admin_token)
d = r.get("data", {})
step("Dashboard 统计可读", r.get("code") == 200, r)
step("今日收入含 15.00", "15.0" in str(d.get("todayIncome")), d.get("todayIncome"))

# 登录限流
s, r = call("POST", "/auth/register", {"username": "brute" + suffix, "password": "Password@123", "name": "brute", "phone": ""})
for i in range(5):
    call("POST", "/auth/login", {"username": "brute" + suffix, "password": "WrongPass@1"})
s, r = call("POST", "/auth/login", {"username": "brute" + suffix, "password": "Password@123"})
step("连续失败 5 次后登录被限流(429)", r.get("code") == 429, r)

# 改密踢会话
s, r = call("PUT", "/users/me/password", {"oldPassword": "Password@123", "newPassword": "NewPassword@456"}, token=access)
step("修改密码成功", r.get("code") == 200, r)
s, r = call("GET", "/users/me", token=access)
step("改密后旧 token 失效(401)", r.get("code") == 401, r)
s, r = call("POST", "/auth/login", {"username": "flow" + suffix, "password": "NewPassword@456"})
step("新密码可登录", r.get("code") == 200, r)
access2 = r["data"]["token"]

# 登出吊销
s, r = call("POST", "/auth/logout", {"refreshToken": None}, token=access2)
step("登出成功", r.get("code") == 200, r)
s, r = call("GET", "/users/me", token=access2)
step("登出后 token 被吊销(401)", r.get("code") == 401, r)

print(json.dumps(results, ensure_ascii=False, indent=1))
print("FLOW_DONE")
