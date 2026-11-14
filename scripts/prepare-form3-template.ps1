$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem
Add-Type -AssemblyName System.IO.Compression
$root = Split-Path -Parent $PSScriptRoot
$source = (Get-ChildItem -LiteralPath $root -Filter '3.*.docx' | Select-Object -First 1).FullName
$target = Join-Path $root 'source/be/src/main/resources/templates/phieu-tiep-nhan-ban-giao-mau.docx'
$temp = Join-Path $env:TEMP 'cfc-form3-template-work.docx'
Copy-Item -LiteralPath $source -Destination $temp -Force
$zip = [IO.Compression.ZipFile]::Open($temp, [IO.Compression.ZipArchiveMode]::Update)
$entry = $zip.GetEntry('word/document.xml')
$reader = New-Object IO.StreamReader($entry.Open(), [Text.Encoding]::UTF8)
$xml = New-Object Xml.XmlDocument
$xml.PreserveWhitespace = $true
$xml.LoadXml($reader.ReadToEnd()); $reader.Dispose()
$ns = New-Object Xml.XmlNamespaceManager($xml.NameTable)
$ns.AddNamespace('w', 'http://schemas.openxmlformats.org/wordprocessingml/2006/main')
$fields = @{
  'No.' = '{{receiptCode}} | {{requestNumber}}'
  'Unique sample code' = '{{sampleCode}}'
  'Received at' = '{{receivedAt}}'
  'Delivered by' = '{{deliveredBy}} | {{receivedBy}}'
  'Name of sample' = '{{sampleName}}'
  'Seal No.' = '{{sealNumber}} | {{sealStatus}}'
  'Details' = '{{conditionDetail}}'
  'Transport' = '{{transportInfo}}'
  'Temperature' = '{{temperature}}'
  'Storage after receipt' = '{{storageCondition}}'
  'Parameters' = '{{requestedParameters}}'
  'Special requests' = '{{specialRequest}}'
  'Reason, conditions' = '{{acceptanceReason}}'
  'Approved by' = '{{approvedBy}}'
}
foreach ($paragraph in $xml.SelectNodes('//w:p', $ns)) {
  $text = $paragraph.InnerText
  foreach ($label in $fields.Keys) {
    if ($text.Contains($label)) {
      $run = $xml.CreateElement('w', 'r', $ns.LookupNamespace('w'))
      $textNode = $xml.CreateElement('w', 't', $ns.LookupNamespace('w'))
      $textNode.SetAttribute('space', 'http://www.w3.org/XML/1998/namespace', 'preserve')
      $textNode.InnerText = '  ' + $fields[$label]
      $run.AppendChild($textNode) | Out-Null
      $paragraph.AppendChild($run) | Out-Null
      break
    }
  }
}
$entry.Delete(); $newEntry = $zip.CreateEntry('word/document.xml')
$writer = New-Object IO.StreamWriter($newEntry.Open(), [Text.Encoding]::UTF8)
$xml.Save($writer); $writer.Dispose(); $zip.Dispose()
New-Item -ItemType Directory -Force (Split-Path $target) | Out-Null
Copy-Item -LiteralPath $temp -Destination $target -Force
Remove-Item -LiteralPath $temp -Force
