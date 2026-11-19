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

    def test_basetoolbox_compatible_normalization(self):
        # Published RFC key only, never a user's credential.
        canonical = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
        decorated = "GEZD8GNBVGY3TQOJQGEZ9DGNBVGY3TQOJQ"
        self.assertEqual(policy.normalize_secret(decorated.lower()), canonical)
        self.assertEqual(policy.normalize_secret(canonical + "B"), canonical)
        self.assertEqual(policy.normalize_secret("A" * 27), "A" * 26)
        self.assertEqual(policy.normalize_secret(canonical + "="), canonical)

    def test_matches_reference_bitstream_decoder(self):
        # Independent equivalent of the website's complete-byte bitstream loop.
        import base64
        alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
        samples = ["GEZD8GNBVGY3TQOJQGEZ9DGNBVGY3TQOJQ", "A" * 29, "A" * 31, "A" * 33, "ab-cd 89" * 8]
        for raw in samples:
            text = "".join(char for char in raw.upper() if char in alphabet)
            bits = "".join(format(alphabet.index(char), "05b") for char in text)
            expected = bytes(int(bits[index:index + 8], 2) for index in range(0, len(bits) - 7, 8))
            normalized = policy.normalize_secret(raw)
            actual = base64.b32decode(normalized + "=" * (-len(normalized) % 8))
            self.assertEqual(actual, expected)

    def test_signing_validator_is_identical(self):
        import inspect
        import textwrap
        workflow = Path(__file__).parents[1].joinpath(".github/workflows/verify-android.yml").read_text()
        start = workflow.index("          def normalize_secret(raw):")
        end = workflow.index("          secret = normalize_secret(", start)
        self.assertEqual(textwrap.dedent(workflow[start:end]).strip(), inspect.getsource(policy.normalize_secret).strip())

    def test_invalid_values_fail_closed(self):
        for value in [" ", "123456", "otpauth://totp/a?secret=x", "AAAAAAAA", "A" * 25, "A" * 104]:
            with self.subTest(value=value), self.assertRaises(ValueError):
                policy.normalize_secret(value)


if __name__ == "__main__":
    unittest.main()
