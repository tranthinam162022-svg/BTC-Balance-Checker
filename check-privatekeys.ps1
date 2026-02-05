# Script chuyen doi private keys sang dia chi Bitcoin va kiem tra so du
# Yeu cau: Python voi thu vien ecdsa va hashlib

param(
    [string]$InputFile = "đời đầu.txt",
    [int]$BatchSize = 20,
    [int]$MaxToCheck = 100,
    [string]$OutputFile = "found-balances.csv",
    [int]$ApiDelay = 200
)

$pythonScript = @'
import hashlib
import sys

def private_key_to_address(private_key_hex):
    try:
        # Import here to avoid issues if not installed
        import ecdsa
        
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
'@

# Luu Python script tam thoi
$pythonScriptPath = "temp_key_converter.py"
$pythonScript | Out-File -FilePath $pythonScriptPath -Encoding UTF8

Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "KIEM TRA SO DU TU PRIVATE KEYS" -ForegroundColor Cyan
Write-Host "===============================================================`n" -ForegroundColor Cyan

# Kiem tra Python va ecdsa
Write-Host "Dang kiem tra moi truong..." -ForegroundColor Yellow

try {
    $pythonVersion = python --version 2>&1
    Write-Host "+ Python: $pythonVersion" -ForegroundColor Green
} catch {
    Write-Host "- Python chua duoc cai dat!" -ForegroundColor Red
    Write-Host "Vui long cai dat Python tu: https://www.python.org/downloads/" -ForegroundColor Yellow
    exit
}

# Kiem tra va cai dat ecdsa neu can
python -c "import ecdsa" 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Dang cai dat thu vien ecdsa..." -ForegroundColor Yellow
    pip install ecdsa
}

# Doc private keys
if (-not (Test-Path $InputFile)) {
    Write-Host "- Khong tim thay file: $InputFile" -ForegroundColor Red
    exit
}

$privateKeys = Get-Content $InputFile | Where-Object { $_.Trim() -ne "" } | Select-Object -First $MaxToCheck

Write-Host "`nTong so private keys: $($privateKeys.Count)" -ForegroundColor Cyan
Write-Host "Se xu ly: $([Math]::Min($privateKeys.Count, $MaxToCheck)) keys`n" -ForegroundColor Cyan

$results = @()
$foundWithBalance = @()
$processed = 0

foreach ($privateKey in $privateKeys) {
    $processed++
    $privateKey = $privateKey.Trim()
    
    Write-Host "[$processed/$($privateKeys.Count)] Đang xử lý: $($privateKey.Substring(0, [Math]::Min(16, $privateKey.Length)))..." -ForegroundColor Gray
    
    # Chuyển đổi private key thành địa chỉ
    $address = python $pythonScriptPath $privateKey
    
    if ($address -like "ERROR:*") {
        Write-Host "  ✗ Lỗi chuyển đổi: $address" -ForegroundColor Red
        continue
    }
    
    Write-Host "  → Địa chỉ: $address" -ForegroundColor Gray
    
    # Kiểm tra số dư
    try {
        $url = "https://blockchain.info/balance?active=$address"
        $response = Invoke-RestMethod -Uri $url -TimeoutSec 10
        
        $data = $response.$address
        $balanceBTC = [math]::Round($data.final_balance / 100000000, 8)
        
        $result = [PSCustomObject]@{
            PrivateKey = $privateKey
            Address = $address
            BalanceBTC = $balanceBTC
            BalanceSatoshi = $data.final_balance
            TotalReceivedBTC = [math]::Round($data.total_received / 100000000, 8)
            TransactionCount = $data.n_tx
            HasBalance = $data.final_balance -gt 0
        }
        
        $results += $result
        
        if ($result.HasBalance) {
            $foundWithBalance += $result
            
            # BAO DONG - TIM THAY VI CO SO DU!
            Write-Host "`n`n" -NoNewline
            Write-Host "================================================================" -ForegroundColor Red -BackgroundColor Yellow
            Write-Host "!!!!! BAO DONG - TIM THAY VI CO SO DU !!!!!" -ForegroundColor Red -BackgroundColor Yellow
            Write-Host "================================================================" -ForegroundColor Red -BackgroundColor Yellow
            Write-Host "`nSO DU: $balanceBTC BTC ($($data.final_balance) satoshi)" -ForegroundColor Green -BackgroundColor Black
            Write-Host "Private Key: $privateKey" -ForegroundColor Yellow -BackgroundColor Black
            Write-Host "Address: $address" -ForegroundColor Cyan -BackgroundColor Black
            Write-Host "Tong nhan: $($result.TotalReceivedBTC) BTC" -ForegroundColor White -BackgroundColor Black
            Write-Host "Giao dich: $($result.TransactionCount)" -ForegroundColor White -BackgroundColor Black
            Write-Host "`n================================================================`n" -ForegroundColor Red -BackgroundColor Yellow
            
            # Phat am thanh bao dong
            for ($i = 0; $i -lt 5; $i++) {
                [Console]::Beep(1000, 300)
                Start-Sleep -Milliseconds 100
                [Console]::Beep(1500, 300)
                Start-Sleep -Milliseconds 100
            }
            
            # Tam dung de nguoi dung xem
            Write-Host "Nhan ENTER de tiep tuc kiem tra cac key con lai..." -ForegroundColor Yellow
            Read-Host
        } else {
            if ($data.n_tx -gt 0) {
                Write-Host "  - Khong con so du (da co $($data.n_tx) giao dich)" -ForegroundColor DarkGray
            } else {
                Write-Host "  - Chua su dung" -ForegroundColor DarkGray
            }
        }
        
        # Delay de tranh rate limiting
        Start-Sleep -Milliseconds $ApiDelay
        
    } catch {
        Write-Host "  Loi kiem tra: $($_.Exception.Message)" -ForegroundColor Red
    }
    
    # Hien thi tien trinh sau moi batch
    if ($processed % $BatchSize -eq 0) {
        Write-Host "`n--- Da xu ly $processed/$($privateKeys.Count) keys ---" -ForegroundColor Cyan
        Write-Host "--- Tim thay $($foundWithBalance.Count) vi co so du ---`n" -ForegroundColor Green
    }
}

# Ket qua cuoi cung
Write-Host "`n===============================================================" -ForegroundColor Green
Write-Host "KET QUA CUOI CUNG" -ForegroundColor Green
Write-Host "===============================================================" -ForegroundColor Green

Write-Host "Da kiem tra: $processed keys" -ForegroundColor White
Write-Host "Tim thay vi co so du: $($foundWithBalance.Count)" -ForegroundColor $(if($foundWithBalance.Count -gt 0){"Green"}else{"Yellow"})

if ($foundWithBalance.Count -gt 0) {
    $totalBalance = ($foundWithBalance | Measure-Object -Property BalanceBTC -Sum).Sum
    Write-Host "Tong so du: $totalBalance BTC" -ForegroundColor Cyan
    
    Write-Host "`n=== CAC VI CO SO DU ===" -ForegroundColor Yellow -BackgroundColor DarkGreen
    foreach ($wallet in $foundWithBalance) {
        Write-Host "`n  Private Key: $($wallet.PrivateKey)" -ForegroundColor Yellow
        Write-Host "  Address: $($wallet.Address)" -ForegroundColor Cyan
        Write-Host "  So du: $($wallet.BalanceBTC) BTC ($($wallet.BalanceSatoshi) satoshi)" -ForegroundColor Green
        Write-Host "  Giao dich: $($wallet.TransactionCount)" -ForegroundColor Gray
    }
    
    # Luu ket qua
    $foundWithBalance | Export-Csv -Path $OutputFile -NoTypeInformation -Encoding UTF8
    Write-Host "`nDa luu cac vi co so du vao: $OutputFile" -ForegroundColor Magenta
}

Write-Host "===============================================================`n" -ForegroundColor Green

# Xoa file Python tam
Remove-Item $pythonScriptPath -ErrorAction SilentlyContinue
