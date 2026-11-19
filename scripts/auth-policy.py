"""Validate the optional TOTP secret before expensive jobs. Never print key material."""
import base64
import hashlib
import hmac
import json
import os
from pathlib import Path


def normalize_secret(raw):
    if raw == "":
        return None
    text = "".join(raw.split()).upper()
    if not text or any(char not in "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567=" for char in text):
        raise ValueError("Invalid TOTP secret: supply a Base32 key (128 to 512 bits), not an OTP or URL")
    unpadded = text.rstrip("=")
    if "=" in unpadded:
        raise ValueError("Invalid TOTP Base32 padding")
    try:
        key = base64.b32decode(unpadded + "=" * (-len(unpadded) % 8))
    except Exception:
        raise ValueError("Invalid TOTP Base32 encoding") from None
    if not 16 <= len(key) <= 64 or base64.b32encode(key).decode().rstrip("=") != unpadded:
        raise ValueError("TOTP key must be canonical Base32 encoding of 128 to 512 bits")
    if "=" in text and text != base64.b32encode(key).decode():
        raise ValueError("Invalid TOTP Base32 padding")
    if len(hmac.new(key, bytes(8), hashlib.sha1).digest()) != 20:
        raise ValueError("TOTP HMAC unavailable")
    return unpadded


def main():
    if os.environ.get("GITHUB_ACTIONS") != "true":
        raise SystemExit("Authentication preflight must run in Actions")
    try:
        secret = normalize_secret(os.environ.get("EASYKIOSK_2FA_TOTP_SECRETS", ""))
    except ValueError as error:
        raise SystemExit(str(error)) from None
    mode = "totp" if secret else "pin"
    with open(os.environ["GITHUB_OUTPUT"], "a") as stream:
        stream.write("mode=" + mode + "\n")
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as stream:
        label = "TOTP 动态密码" if secret else "固定密码"
        stream.write("### 认证配置校验\n认证模式：" + label + "。配置校验通过。\n")
    print("Authentication preflight passed: " + mode)


if __name__ == "__main__":
    main()
