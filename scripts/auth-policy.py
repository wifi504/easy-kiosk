"""Validate the optional TOTP secret before expensive jobs. Never print key material."""
import base64
import hashlib
import hmac
import json
import os
import re
from pathlib import Path


def normalize_secret(raw):
    if raw == "":
        return None
    if raw.strip().lower().startswith(("otpauth:", "http:", "https:")):
        raise ValueError("Supply the secret itself, not a URL")
    # Match BaseToolbox's I()/L() decoder: discard non-alphabet characters,
    # consume complete bytes, and ignore remaining bits. Never guess ASCII/Base64.
    # Reference: https://basetoolbox.com/zh/2fa/
    text = re.sub(r"[^A-Z2-7]", "", raw.upper())
    alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    output = bytearray()
    buffer = bits = 0
    for char in text:
        buffer = (buffer << 5) | alphabet.index(char)
        bits += 5
        if bits >= 8:
            bits -= 8
            output.append((buffer >> bits) & 255)
        buffer &= (1 << bits) - 1
    key = bytes(output)
    if not 16 <= len(key) <= 64:
        raise ValueError("TOTP key must decode to 128 to 512 bits after normalization")
    if len(hmac.new(key, bytes(8), hashlib.sha1).digest()) != 20:
        raise ValueError("TOTP HMAC unavailable")
    # Package only a canonical encoding of the exact decoded bytes.
    return base64.b32encode(key).decode("ascii").rstrip("=")


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
