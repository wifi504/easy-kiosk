import importlib.util
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location("auth_policy", Path(__file__).with_name("auth-policy.py"))
policy = importlib.util.module_from_spec(spec)
spec.loader.exec_module(policy)


class AuthSecretTest(unittest.TestCase):
    def test_empty_disables_totp(self):
        self.assertIsNone(policy.normalize_secret(""))

    def test_rfc_secret_and_formatting(self):
        expected = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
        self.assertEqual(policy.normalize_secret(expected.lower() + " \n"), expected)
        padded = "IFBEGRCFIZDUQSKKJNGE2TSPKA======"
        self.assertEqual(policy.normalize_secret(padded), padded.rstrip("="))

    def test_invalid_values_fail_closed(self):
        for value in [" ", "123456", "otpauth://totp/a?secret=x", "AAAAAAAA", "A" * 27, "A" * 104, "A" * 32 + "="]:
            with self.subTest(value=value), self.assertRaises(ValueError):
                policy.normalize_secret(value)


if __name__ == "__main__":
    unittest.main()
