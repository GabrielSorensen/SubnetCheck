IF NOT Exist %CD%\ mkdir %CD%\
FOR /l %%i IN (44,1,46) DO (  
	 ping -a -n 1 10.232.224.%%i 
	 nbtstat -a 10.232.224.%%i 
) > %CD%\results\%%i.txt
