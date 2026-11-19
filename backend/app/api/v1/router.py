from fastapi import APIRouter

from app.api.v1 import analytics, budgets, categories, health, me, transactions

api_router = APIRouter()
api_router.include_router(health.router)
api_router.include_router(me.router)
api_router.include_router(categories.router)
api_router.include_router(transactions.router)
api_router.include_router(budgets.router)
api_router.include_router(analytics.router)
