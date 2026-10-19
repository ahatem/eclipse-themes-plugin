# Disposable R9 helper: drives the native Windows file dialog that the preference page opens.
# Win32 messages set the file name, switch filters and press Open/Cancel (these controls expose no UIA patterns);
# UIA only reads the item list shown for each filter. Logs the filters, each filter's listing, then opens -Path (or cancels for "-").
param([string]$Title, [string]$Path, [string]$Log)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName UIAutomationClient, UIAutomationTypes
Add-Type @'
using System; using System.Text; using System.Runtime.InteropServices;
public static class W {
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern IntPtr FindWindow(string cls, string title);
  [DllImport("user32.dll")] public static extern IntPtr GetDlgItem(IntPtr dlg, int id);
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern IntPtr FindWindowEx(IntPtr parent, IntPtr after, string cls, string title);
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern IntPtr SendMessage(IntPtr h, int msg, IntPtr w, string l);
  [DllImport("user32.dll", CharSet=CharSet.Unicode)] public static extern IntPtr SendMessage(IntPtr h, int msg, IntPtr w, StringBuilder l);
  [DllImport("user32.dll")] public static extern IntPtr SendMessage(IntPtr h, int msg, IntPtr w, IntPtr l);
  [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, int msg, IntPtr w, IntPtr l);
  [DllImport("user32.dll")] public static extern IntPtr GetParent(IntPtr h);
  [DllImport("user32.dll")] public static extern int GetDlgCtrlID(IntPtr h);
}
'@
function L($s) { Add-Content -Path $Log -Value $s -Encoding utf8 }
$A = [System.Windows.Automation.AutomationElement]
$T = [System.Windows.Automation.TreeScope]
$WM_SETTEXT = 0x000C; $BM_CLICK = 0x00F5; $WM_COMMAND = 0x0111
$CB_GETCOUNT = 0x0146; $CB_GETLBTEXT = 0x0148; $CB_SETCURSEL = 0x014E; $CBN_SELCHANGE = 1
try {
  $h = [IntPtr]::Zero
  for ($i = 0; $i -lt 120 -and $h -eq [IntPtr]::Zero; $i++) { $h = [W]::FindWindow('#32770', $Title); if ($h -eq [IntPtr]::Zero) { Start-Sleep -Milliseconds 250 } }
  if ($h -eq [IntPtr]::Zero) { L "UIA no-dialog title=$Title"; exit 1 }
  L "UIA dialog title=$Title hwnd=$h"
  Start-Sleep -Milliseconds 800
  if ($Path -eq '-') { [void][W]::SendMessage([W]::GetDlgItem($h, 2), $BM_CLICK, [IntPtr]::Zero, [IntPtr]::Zero); L 'UIA cancelled'; exit 0 }
  # File name edit: ComboBoxEx32(1148) > ComboBox > Edit.
  $cbx = [W]::GetDlgItem($h, 1148)
  $edit = [W]::FindWindowEx([W]::FindWindowEx($cbx, [IntPtr]::Zero, 'ComboBox', $null), [IntPtr]::Zero, 'Edit', $null)
  if ($edit -eq [IntPtr]::Zero) { $edit = $cbx }
  $openBtn = [W]::GetDlgItem($h, 1)
  function SetName($v) { [void][W]::SendMessage($edit, $WM_SETTEXT, [IntPtr]::Zero, $v) }
  function Open { [void][W]::SendMessage($openBtn, $BM_CLICK, [IntPtr]::Zero, [IntPtr]::Zero) }
  $types = [W]::GetDlgItem($h, 1136)
  function SelectFilter($k) {
    # Keyboard selection makes the combo raise its own selection notifications, which the shell view honours.
    [void][W]::SendMessage($types, 0x0007, [IntPtr]::Zero, [IntPtr]::Zero)  # WM_SETFOCUS
    [void][W]::SendMessage($types, 0x0100, [IntPtr]0x24, [IntPtr]::Zero); [void][W]::SendMessage($types, 0x0101, [IntPtr]0x24, [IntPtr]::Zero)  # Home
    for ($j = 0; $j -lt $k; $j++) { [void][W]::SendMessage($types, 0x0100, [IntPtr]0x28, [IntPtr]::Zero); [void][W]::SendMessage($types, 0x0101, [IntPtr]0x28, [IntPtr]::Zero) }  # Down
  }
  function Listing {
    $dlg = $A::FromHandle($h)
    $list = $dlg.FindFirst($T::Descendants, (New-Object System.Windows.Automation.PropertyCondition($A::ClassNameProperty, 'UIItemsView')))
    if (-not $list) { return '<no items view>' }
    (@($list.FindAll($T::Descendants, (New-Object System.Windows.Automation.PropertyCondition($A::ControlTypeProperty, [System.Windows.Automation.ControlType]::ListItem))) | ForEach-Object { $_.Current.Name }) | Sort-Object) -join ', '
  }
  # Navigate to the QA folder first so each filter's listing can be observed.
  SetName (Split-Path $Path -Parent); Open; Start-Sleep -Milliseconds 1500
  $n = [int][W]::SendMessage($types, $CB_GETCOUNT, [IntPtr]::Zero, [IntPtr]::Zero)
  $names = for ($k = 0; $k -lt $n; $k++) { $sb = New-Object System.Text.StringBuilder 512; [void][W]::SendMessage($types, $CB_GETLBTEXT, [IntPtr]$k, $sb); $sb.ToString() }
  L ("UIA filters=" + ($names -join ' | '))
  for ($k = 0; $k -lt $n; $k++) { SelectFilter $k; Start-Sleep -Milliseconds 1200; L ("UIA filter='" + $names[$k] + "' lists=" + (Listing)) }
  # Leave the first (default) filter selected, then open the requested file.
  SelectFilter 0; Start-Sleep -Milliseconds 600
  SetName $Path; Open
  L "UIA opened $Path"
} catch { L "UIA error line=$($_.InvocationInfo.ScriptLineNumber) $_"; exit 2 }
