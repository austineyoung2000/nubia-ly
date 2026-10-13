package com.elitedarkkaiser.nubialy;
public final class ClockEnabledSmokeTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[]args){String id="12345678-1234-1234-1234-123456789abc";
 String receipt="nubia-ly-clock-enabled-v1\n"+id+"\nclock-enabled\n"+id+"\n";
 check(id.equals(ClockEnabledPolicy.parse(receipt,id)));check(ClockEnabledPolicy.parse(receipt.replace("clock-enabled\n","lifecycle-only\n"),id)==null);
 check(ClockEnabledPolicy.parse(receipt,"00000000-1234-1234-1234-123456789abc")==null);check(PreviewPolicy.parse(receipt,id,1000)==null);
 check(ClockEnabledPolicy.allowStartup(0)&&ClockEnabledPolicy.allowStartup(2)&&!ClockEnabledPolicy.allowStartup(3));
 PreviewLifecycle s=new PreviewLifecycle();long ticket=s.begin(true);check(s.activate(ticket,new PreviewPolicy.Permit(id,16000),1000));
 check(s.validate(ticket,new PreviewPolicy.Permit(id,21000),6000));check(s.state(17000)==PreviewLifecycle.Phase.ACTIVE);
 s.stop();check(!s.validate(ticket,new PreviewPolicy.Permit(id,25000),10000));
 ticket=s.begin();check(s.activate(ticket,new PreviewPolicy.Permit(id,31000),1000));check(!s.validate(ticket,new PreviewPolicy.Permit(id,32000),2000));
 ticket=s.begin(true);check(s.activate(ticket,new PreviewPolicy.Permit(id,16000),1000));check(s.state(16000)==PreviewLifecycle.Phase.EXPIRED);check(!s.validate(ticket,new PreviewPolicy.Permit(id,30000),16001));
 System.out.println("Enabled clock checks passed: boot authority, distinct purpose, restart cutoff, healthy continuation, stop/late-read fencing and unchanged preview deadline");}}
