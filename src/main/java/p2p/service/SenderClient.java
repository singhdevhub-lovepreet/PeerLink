package service;

class SenderService {

    public static HashMap<Integer, String> portIpMap;

    SenderService(){
        ports = new HashSet<>();
    }

    public void sendFile(){
        Integer port = null;
        while(true){
            Integer generatedPort = UploadUtils.generateCode();
            if(!ports.contains(port)){
                port = generatedPort;
                portIpMap.put(port, ipAddress);
                break;
            }
        }
        
    }

    public void uploadFile(){

    }

    

}