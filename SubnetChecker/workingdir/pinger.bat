IF NOT Exist %CD%\ mkdir %CD%\
FOR /l %%i IN (1,1,255) DO (  
	  ping -a -n 1 10.232.224.%%i 
	 nbtstat -a 10.232.224.%%i 
) > %CD%\10.232.224.\ %%i.txt
