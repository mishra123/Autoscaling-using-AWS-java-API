import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;


public class benchmark 
{
	public static void main(String args[]) throws IOException
	{
		Properties properties = new Properties();
		properties.load(benchmark.class
				.getResourceAsStream("/AwsCredentials.properties"));

		BasicAWSCredentials bawsc = new BasicAWSCredentials(
				properties.getProperty("accessKey"),
				properties.getProperty("secretKey"));

		AmazonEC2Client ec2 = new AmazonEC2Client(bawsc);

		// Create Instance Request
		RunInstancesRequest runInstancesRequest = new RunInstancesRequest();
		
		// Configure Instance Result
		RunInstancesResult runInstancesResult = ec2
				.runInstances(runInstancesRequest);
		
		Instance instance = runInstancesResult.getReservation().getInstances()
				.get(0);
		
		runInstancesRequest.withImageId("ami-2b7b2c42")
		.withInstanceType("m1.medium").withMinCount(1).withMaxCount(1)
		.withKeyName("ec2privatekey");
		
		String medium = instance.getInstanceId();
		
DescribeInstanceStatusRequest describeInstanceRequest = new DescribeInstanceStatusRequest().withInstanceIds(medium);
		
		DescribeInstanceStatusResult describeInstanceResult = ec2.describeInstanceStatus(describeInstanceRequest);
		List<InstanceStatus> state = describeInstanceResult.getInstanceStatuses();
		while (state.size()< 1) 
		{ 
		    // Do nothing, just wait, have thread sleep if needed
		    describeInstanceResult = ec2.describeInstanceStatus(describeInstanceRequest);
		    state = describeInstanceResult.getInstanceStatuses();
		}
		String status =  state.get(0).getInstanceState().getName();
		       
		if(status.equals("running"))
		{
			AmazonEC2Client amazonEC2Client = new AmazonEC2Client(bawsc);	
			List<Reservation> reservations = amazonEC2Client.describeInstances().getReservations();
			
			 int reservationCount = reservations.size();
			   
			  for(int i = 0; i < reservationCount; i++) 
			  {   
			      List<Instance> i_all = reservations.get(i).getInstances();
			      int instanceCount = i_all.size();
			      for(int j = 0; j < instanceCount; j++) 
			      {
			          Instance medium_instance = i_all.get(j);
			          String i_id=medium_instance.getInstanceId();
			          if(medium.equals(i_id))
			          {
			        	  System.out.println("Public DNS: "+medium_instance.getPublicDnsName());
			          try 
			          {
			        	  
			        	  for (int k = 0; k < 1; k++) 
			        	  {
			        	  JSch jsch = new JSch();
			              String host = "ec2-54-224-97-89.compute-1.amazonaws.com";
			              String keyFile = "lib/ec2privatekey.pem";
			              jsch.addIdentity(keyFile);
			              Session session = jsch.getSession("ubuntu", host, 22);
			              java.util.Properties config = new java.util.Properties(); 
			              config.put("StrictHostKeyChecking", "no");
			              session.setConfig(config);
			              session.connect();
			              
			              ChannelExec channelExec = (ChannelExec) session.openChannel("exec");
			              
			              //InputStream in = channelExec.getInputStream();
			              
			              channelExec
			                .setCommand("cd $HOME/benchmark && sudo ./seige_bench.sh "+medium_instance.getPublicDnsName());
			              channelExec.connect();

			              
			               InputStream in = channelExec.getInputStream();  
			                     byte[] tmp=new byte[1024];  
			                     while(true){  
			                         while(in.available()>0){  
			                           int b=in.read(tmp, 0, 1024);  
			                           if(b<0)break;  
			                           System.out.print(new String(tmp, 0, b));  
			                         }  
			                         if(channelExec.isClosed()){  
			                           System.out.println("exit-status: "+channelExec.getExitStatus());  
			                           break;  
			                         }  
			                         try
			                         {
			                        	 Thread.sleep(1000);
			                         }
			                         
			                         catch(Exception ee1)
			                         {
			                        	System.out.print("Inside medium" + ee1);
			                        	 } 
			                         
			                     }
			        	  }
			          }
			     catch(Exception exception)
		          {
		                        	 System.out.println("Exception"  + exception);
		          }
			                     }
			          }
			          


	}

}
}
}