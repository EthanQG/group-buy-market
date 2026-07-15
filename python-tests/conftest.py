import pytest

BASE_URL = "http://localhost:8091/api/v1/gbm/trade"

@pytest.fixture
def base_url():
    return BASE_URL
