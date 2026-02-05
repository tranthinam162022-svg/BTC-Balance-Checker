import hashlib
import sys

def private_key_to_address(private_key_hex):
    try:
        # Import here to avoid issues if not installed
        import ecdsa # type: ignore
        
        # Convert hex private key to bytes
        private_key_bytes = bytes.fromhex(private_key_hex)
        
        # Create signing key from private key
        signing_key = ecdsa.SigningKey.from_string(private_key_bytes, curve=ecdsa.SECP256k1)
        verifying_key = signing_key.get_verifying_key()
        
        # Get public key (uncompressed format)
        public_key = b'\x04' + verifying_key.to_string()
        
        # SHA256 hash of public key
        sha256_hash = hashlib.sha256(public_key).digest()
        
        # RIPEMD160 hash
        ripemd160_hash = hashlib.new('ripemd160', sha256_hash).digest()
        
        # Add version byte (0x00 for mainnet)
        versioned_hash = b'\x00' + ripemd160_hash
        
        # Double SHA256 for checksum
        checksum = hashlib.sha256(hashlib.sha256(versioned_hash).digest()).digest()[:4]
        
        # Combine and encode to base58
        address_bytes = versioned_hash + checksum
        
        # Base58 encoding
        alphabet = '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz'
        num = int.from_bytes(address_bytes, 'big')
        encoded = ''
        while num > 0:
            num, remainder = divmod(num, 58)
            encoded = alphabet[remainder] + encoded
        
        # Add leading '1's for leading zero bytes
        for byte in address_bytes:
            if byte == 0:
                encoded = '1' + encoded
            else:
                break
                
        return encoded
    except Exception as e:
        return f"ERROR: {str(e)}"

if __name__ == "__main__":
    private_key = sys.argv[1] if len(sys.argv) > 1 else ""
    if private_key:
        print(private_key_to_address(private_key))
