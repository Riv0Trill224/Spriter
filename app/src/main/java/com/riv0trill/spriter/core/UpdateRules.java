package com.riv0trill.spriter.core;
import java.net.URI;
import java.util.Locale;
public final class UpdateRules {
    public static boolean allowedDownload(String value) {
        try {
            URI u=new URI(value);String h=u.getHost();
            if(!"https".equalsIgnoreCase(u.getScheme()) || u.getUserInfo()!=null || (u.getPort()!=-1&&u.getPort()!=443) || h==null)return false;
            h=h.toLowerCase(Locale.ROOT);
            return h.equals("api.github.com") || h.equals("github.com") || h.equals("release-assets.githubusercontent.com") || h.equals("objects.githubusercontent.com");
        }catch(Exception e){return false;}
    }
    public static boolean validMetadata(String pkg,long code,long installed,long size,String sha) {
        return "com.riv0trill.spriter".equals(pkg) && code>installed && size>0 && size<=64L*1024*1024
                && sha!=null && sha.matches("[a-fA-F0-9]{64}");
    }
}
