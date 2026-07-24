"""
健身房 AI 助手 — Python 端
接收 Java ai-chat 服务发来的消息，调用 DeepSeek API 并返回结果
启动: uvicorn ai-chat:app --host 0.0.0.0 --port 8000 --reload
"""
import json
import os
import httpx
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from dotenv import load_dotenv
import asyncio

load_dotenv()  # 自动加载同目录下的 .env 文件

app = FastAPI(title="Gym AI Python Service", version="1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "sk-your-key-here")
DEEPSEEK_URL = "https://api.deepseek.com/v1/chat/completions"


async def call_deepseek(messages: list, stream: bool = False):
    """调用 DeepSeek API"""
    async with httpx.AsyncClient(timeout=60.0) as client:
        headers = {
            "Authorization": f"Bearer {DEEPSEEK_API_KEY}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": "deepseek-chat",
            "messages": messages,
            "stream": stream,
            "temperature": 0.7,
            "max_tokens": 800,
        }

        response = await client.post(DEEPSEEK_URL, json=payload, headers=headers)
        response.raise_for_status()
        data = response.json()
        return data["choices"][0]["message"]["content"]


async def call_deepseek_stream(messages: list):
    """调用 DeepSeek API — 流式输出"""
    async with httpx.AsyncClient(timeout=60.0) as client:
        headers = {
            "Authorization": f"Bearer {DEEPSEEK_API_KEY}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": "deepseek-chat",
            "messages": messages,
            "stream": True,
            "temperature": 0.7,
            "max_tokens": 800,
        }

        async with client.stream("POST", DEEPSEEK_URL, json=payload, headers=headers) as response:
            response.raise_for_status()
            async for line in response.aiter_lines():
                if line.startswith("data: "):
                    data_str = line[6:]
                    if data_str == "[DONE]":
                        yield "data: [DONE]\n\n"
                        break
                    try:
                        chunk = json.loads(data_str)
                        delta = chunk["choices"][0].get("delta", {})
                        content = delta.get("content", "")
                        if content:
                            yield f"data: {json.dumps({'content': content})}\n\n"
                    except json.JSONDecodeError:
                        continue


@app.post("/chat")
async def chat(request: dict):
    """
    POST /chat
    Body: { "messages": [...], "stream": false }
    返回: { "reply": "..." }
    """
    messages = request.get("messages", [])
    stream = request.get("stream", False)

    if not messages:
        return {"reply": "请输入您的问题。"}

    try:
        if stream:
            # 流式响应
            return StreamingResponse(
                call_deepseek_stream(messages),
                media_type="text/event-stream",
                headers={"X-Accel-Buffering": "no"},
            )
        else:
            reply = await call_deepseek(messages, stream=False)
            return {"reply": reply}
    except httpx.HTTPStatusError as e:
        return {"reply": f"AI 接口调用失败，状态码: {e.response.status_code}，请稍后重试。"}
    except Exception as e:
        return {"reply": f"服务异常: {str(e)}，请稍后重试。"}


@app.get("/health")
async def health():
    return {"status": "ok", "service": "gym-ai-python"}
