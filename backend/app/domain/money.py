"""Monetary invariants mirrored from the Android MVP (paise / INR)."""

MAX_AMOUNT_MINOR = 99_999_999_999  # ₹99,99,99,999.99
MAX_DESCRIPTION_LENGTH = 120
SUPPORTED_CURRENCY = "INR"


def validate_positive_amount_minor(amount_minor: int) -> None:
    if amount_minor <= 0:
        raise ValueError("amount_minor must be greater than zero")
    if amount_minor > MAX_AMOUNT_MINOR:
        raise ValueError("amount_minor exceeds the supported maximum")


def validate_non_negative_amount_minor(amount_minor: int) -> None:
    if amount_minor < 0:
        raise ValueError("amount_minor must be greater than or equal to zero")
    if amount_minor > MAX_AMOUNT_MINOR:
        raise ValueError("amount_minor exceeds the supported maximum")


def validate_currency(currency: str) -> str:
    normalized = currency.upper()
    if normalized != SUPPORTED_CURRENCY:
        raise ValueError(f"Only {SUPPORTED_CURRENCY} is supported")
    return normalized
