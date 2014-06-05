IF NOT Exist %CD%\ mkdir %CD%\
FOR /l %%i IN (44,1,46) DO (  
	 ping -a -n 1 10.232.224.%%i 
	 nbtstat -a 10.232.224.%%i 
	 wmic /Output:out%%i.txt /Node:10.232.224. Path Win32_NetworkAdapterConfiguration Get DHCPEnabled,DNSHostName,MACAddress,IPAddress
) > %CD%\results\%%i.txt
