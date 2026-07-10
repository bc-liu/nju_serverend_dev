"""全局配置，从 .env 加载"""
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # LLM
    llm_api_key: str = ""
    llm_base_url: str = "https://api.deepseek.com"
    llm_model: str = "deepseek-v4-flash"

    # Java 后端
    backend_base_url: str = "http://localhost:8080"

    # Redis
    redis_url: str = "redis://localhost:6379/1"

    # Agent 服务
    agent_port: int = 8000

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
