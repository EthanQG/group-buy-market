import requests
import uuid
import time
import pytest


def test_lock_order_success(base_url):
    """测试用例1: 锁单成功"""
    url = f"{base_url}/lock_market_pay_order"
    
    payload = {
        "userId": "user001",
        "source": "APP",
        "channel": "ALI",
        "goodsId": "1001",
        "activityId": 1001,
        "outTradeNo": str(uuid.uuid4()),
        "teamId": None,
        "notifyConfigVO": {
            "notifyType": "HTTP",
            "notifyUrl": "http://localhost:8080/callback"
        }
    }
    
    response = requests.post(url, json=payload)
    
    assert response.status_code == 200
    assert response.json()["code"] == "0000" or response.json()["code"] == "0000"
    assert "orderId" in response.json()["data"]


def test_lock_order_missing_params(base_url):
    """测试用例2: 锁单 - 缺少参数"""
    url = f"{base_url}/lock_market_pay_order"
    
    payload = {
        "userId": "user001"
        # 缺少 source, channel, goodsId, activityId, outTradeNo 等必填参数
    }
    
    response = requests.post(url, json=payload)
    
    assert response.status_code == 200
    assert response.json()["code"] == "0001"  # 参数非法


def test_settlement_order(base_url):
    """测试用例3: 结算接口"""
    url = f"{base_url}/settlement_market_pay_order"
    
    payload = {
        "userId": "user001",
        "source": "APP",
        "channel": "ALI",
        "outTradeNo": "order_test_001",
        "outTradeTime": int(time.time() * 1000)  # 当前时间戳
    }
    
    response = requests.post(url, json=payload)
    
    assert response.status_code == 200
    assert "code" in response.json()


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
