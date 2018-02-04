#Adresy

##Java

35.226.175.93:8080

35.225.46.100:8080

##Bazy

sa:pass 

35.193.184.100:8082 

35.188.40.119:8082 


#Requesty


curl -X GET http://35.190.18.169:8080/api/item

curl -X POST http://35.190.18.169:8080/api/item -H 'Content-Type: application/json' -d '{"name":"sword2","description":"meele"}'

curl -X DELETE http://35.190.18.169:8080/api/item 

curl -X DELETE http://35.190.18.169:8080/api/item/1

