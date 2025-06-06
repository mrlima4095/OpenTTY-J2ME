if (abc !startswith a) echo STARTSWITH OK
if (abc startswith z) echo NOT STARTSWITH OK
if (abc !endswith c) echo ENDSWITH OK
if (abc endswith x) echo NOT ENDSWITH OK
if (abc !contains b) echo CONTAINS OK
if (abc contains z) echo NOT CONTAINS OK
if (abc != abc) echo EQUALS OK
if (abc == xyz) echo NOT EQUALS OK
if (123 != 123) echo NUM == OK
if (123 == 321) echo NUM != OK
if (100 < 50) echo NUM > OK
if (50 > 100) echo NUM < OK
if (100 >= 101) echo NUM >= OK
if (100 <= 99) echo NUM <= OK
if (9999 contains a) echo FINAL TEST PASSED
