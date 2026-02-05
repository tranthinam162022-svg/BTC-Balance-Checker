# Script kiểm tra số dư hàng loạt địa chỉ ví Bitcoin
# Sử dụng: .\check-batch-wallets.ps1 -Addresses "address1,address2,address3"
# Hoặc: .\check-batch-wallets.ps1 -AddressFile "addresses.txt"

param(
    [string]$Addresses = "",
    [string]$AddressFile = "",
    [string]$OutputFile = "wallet-results.csv"
)

function Get-BTCBalance {
    param([string]$Address)
    
    try {
        $url = "https://blockchain.info/balance?active=$Address"
        $response = Invoke-RestMethod -Uri $url -TimeoutSec 10
        
        $data = $response.$Address
        $balanceBTC = [math]::Round($data.final_balance / 100000000, 8)
        $totalReceivedBTC = [math]::Round($data.total_received / 100000000, 8)
        
        return [PSCustomObject]@{
            Address = $Address
            Status = "Success"
            BalanceBTC = $balanceBTC
            BalanceSatoshi = $data.final_balance
            TotalReceivedBTC = $totalReceivedBTC
            TotalReceivedSatoshi = $data.total_received
            TransactionCount = $data.n_tx
            HasBalance = $data.final_balance -gt 0
        }
    }
    catch {
        return [PSCustomObject]@{
            Address = $Address
            Status = "Error"
            BalanceBTC = 0
            BalanceSatoshi = 0
            TotalReceivedBTC = 0
            TotalReceivedSatoshi = 0
            TransactionCount = 0
            HasBalance = $false
            Error = $_.Exception.Message
        }
    }
}

# Lấy danh sách địa chỉ
$addressList = @()

if ($AddressFile -and (Test-Path $AddressFile)) {
    Write-Host "Đọc địa chỉ từ file: $AddressFile" -ForegroundColor Cyan
    $addressList = Get-Content $AddressFile | Where-Object { $_.Trim() -ne "" }
}
elseif ($Addresses) {
    $addressList = $Addresses -split ',' | ForEach-Object { $_.Trim() }
}
else {
    Write-Host "Nhập danh sách địa chỉ Bitcoin (mỗi dòng một địa chỉ, nhập dòng trống để kết thúc):" -ForegroundColor Yellow
    while ($true) {
        $addr = Read-Host "Địa chỉ"
        if ([string]::IsNullOrWhiteSpace($addr)) { break }
        $addressList += $addr.Trim()
    }
}

if ($addressList.Count -eq 0) {
    Write-Host "Không có địa chỉ nào để kiểm tra!" -ForegroundColor Red
    exit
}

Write-Host "`n═══════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "Bắt đầu kiểm tra $($addressList.Count) địa chỉ ví..." -ForegroundColor Green
Write-Host "═══════════════════════════════════════════════════════════════`n" -ForegroundColor Green

$results = @()
$totalBalance = 0

foreach ($i in 0..($addressList.Count - 1)) {
    $address = $addressList[$i]
    Write-Host "[$($i + 1)/$($addressList.Count)] Đang kiểm tra: $address" -ForegroundColor Cyan
    
    $result = Get-BTCBalance -Address $address
    $results += $result
    
    if ($result.Status -eq "Success") {
        $totalBalance += $result.BalanceBTC
        
        if ($result.HasBalance) {
            Write-Host "  ✅ CÓ SỐ DƯ: $($result.BalanceBTC) BTC ($($result.BalanceSatoshi) satoshi)" -ForegroundColor Green
            Write-Host "     Tổng nhận: $($result.TotalReceivedBTC) BTC | Giao dịch: $($result.TransactionCount)" -ForegroundColor Gray
        }
        else {
            Write-Host "  ⚪ KHÔNG CÓ SỐ DƯ (Tổng nhận: $($result.TotalReceivedBTC) BTC, Giao dịch: $($result.TransactionCount))" -ForegroundColor Yellow
        }
    }
    else {
        Write-Host "  ❌ LỖI: $($result.Error)" -ForegroundColor Red
    }
    
    # Delay để tránh rate limiting
    if ($i -lt $addressList.Count - 1) {
        Start-Sleep -Milliseconds 500
    }
}

# Xuất kết quả
Write-Host "`n═══════════════════════════════════════════════════════════════" -ForegroundColor Green
Write-Host "KẾT QUẢ TỔNG HỢP" -ForegroundColor Green
Write-Host "═══════════════════════════════════════════════════════════════" -ForegroundColor Green

$withBalance = $results | Where-Object { $_.HasBalance -eq $true }
$successCount = ($results | Where-Object { $_.Status -eq "Success" }).Count
$errorCount = ($results | Where-Object { $_.Status -eq "Error" }).Count

Write-Host "Tổng số địa chỉ kiểm tra: $($addressList.Count)" -ForegroundColor White
Write-Host "Thành công: $successCount | Lỗi: $errorCount" -ForegroundColor White
Write-Host "Địa chỉ có số dư: $($withBalance.Count)" -ForegroundColor Green
Write-Host "Tổng số dư: $totalBalance BTC" -ForegroundColor Cyan

if ($withBalance.Count -gt 0) {
    Write-Host "`n📊 CHI TIẾT CÁC VÍ CÓ SỐ DƯ:" -ForegroundColor Yellow
    foreach ($wallet in $withBalance) {
        Write-Host "  • $($wallet.Address): $($wallet.BalanceBTC) BTC" -ForegroundColor Green
    }
}

# Lưu vào file CSV
$results | Export-Csv -Path $OutputFile -NoTypeInformation -Encoding UTF8
Write-Host "`n💾 Kết quả đã được lưu vào: $OutputFile" -ForegroundColor Magenta
Write-Host "═══════════════════════════════════════════════════════════════`n" -ForegroundColor Green
