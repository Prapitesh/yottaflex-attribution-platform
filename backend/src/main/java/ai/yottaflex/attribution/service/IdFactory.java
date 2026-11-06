package ai.yottaflex.attribution.service;

import java.util.concurrent.atomic.AtomicLong;

public final class IdFactory {
    private final AtomicLong counter=new AtomicLong(1);
    private final String prefix;
    private final int width;
    public IdFactory(String prefix,int width){this.prefix=prefix;this.width=width;}
    public String next(){return prefix+String.format("%0"+width+"d",counter.getAndIncrement());}
    public static String contactTouch(String contactId,int sequence){return "TOUCH-"+contactId+"-"+String.format("%06d",sequence);}
}
