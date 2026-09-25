package com.riv0trill.spriter.core;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ReleaseTest {
    @Test public void populationUsesSpaceAndCanExceedFive() {
        Random random=new Random(42);Set<Integer> seen=new HashSet<>();
        for(int i=0;i<1000;i++) {
            int n=Population.choose(305,640,450,72,random);
            assertTrue(n>=1 && n<=21);seen.add(n);
        }
        assertTrue(Collections.max(seen)>5);assertTrue(seen.size()>5);
    }
    @Test public void populationRespectsSmallCollectionsAndUnavailableLayout() {
        Random random=new Random(1);
        for(int i=0;i<100;i++)assertEquals(1,Population.choose(1,640,480,72,random));
        assertEquals(0,Population.choose(0,640,480,72,random));
        assertEquals(0,Population.choose(305,0,480,72,random));
        assertEquals(0,Population.choose(305,640,480,0,random));
    }
    @Test public void downloadRejectsUntrustedOrigins() {
        for(String url:new String[]{"http://api.github.com/x","https://github.com.evil.test/x","https://github.com@evil.test/x","https://evil.test@github.com/x","file:///tmp/apk","https://github.com:444/x","//github.com/x"})assertFalse(url,UpdateRules.allowedDownload(url));
        assertTrue(UpdateRules.allowedDownload("https://api.github.com/repos/Riv0Trill224/Spriter/releases"));
        assertTrue(UpdateRules.allowedDownload("https://release-assets.githubusercontent.com/x?token=test"));
    }
    @Test public void metadataRejectsDowngradesWrongPackagesAndOversizedDownloads() {
        String sha=String.join("",Collections.nCopies(64,"a"));
        assertTrue(UpdateRules.validMetadata("com.riv0trill.spriter",101,100,1024,sha));
        assertFalse(UpdateRules.validMetadata("com.riv0trill.spriter",100,100,1024,sha));
        assertFalse(UpdateRules.validMetadata("other",101,100,1024,sha));
        assertFalse(UpdateRules.validMetadata("com.riv0trill.spriter",101,100,67108865,sha));
        assertFalse(UpdateRules.validMetadata("com.riv0trill.spriter",101,100,0,sha));
        assertFalse(UpdateRules.validMetadata("com.riv0trill.spriter",101,100,1024,"bad"));
    }
}
