import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerResult;
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.autoscaling.*;
import com.amazonaws.services.autoscaling.model.*;
//import com.amazonaws.typica.autoscale.jaxb.model.CreateLaunchConfiguration;
import com.amazonaws.services.elasticbeanstalk.model.LaunchConfiguration;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.Topic;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class anoopAutoScaling {
	private static final String String = null;

	public static void main(String[] args) throws IOException,
			InterruptedException, Exception {
		// Load the Properties File with AWS Credentials
		Properties properties = new Properties();
		properties.load(anoopAutoScaling.class
				.getResourceAsStream("/AwsCredentials.properties"));

		BasicAWSCredentials bawsc = new BasicAWSCredentials(
				properties.getProperty("accessKey"),
				properties.getProperty("secretKey"));
		// AmazonEC2Client ec2 = new AmazonEC2Client(bawsc);
		System.out.println("Hi");
		// Create an Amazon EC2 Client
		// Create Instance Request
		AmazonElasticLoadBalancingClient elb = new AmazonElasticLoadBalancingClient(
				bawsc);
		// elb.createLoadBalancer(createLoadBalancerRequest);

		CreateLoadBalancerRequest lbRequest = new CreateLoadBalancerRequest();
		lbRequest.setLoadBalancerName("ProgAnoopLoadBalancer1");
		List<String> elbnames = new ArrayList<String>();
		elbnames.add("ProgAnoopLoadBalancer1");
		List<Listener> listeners = new ArrayList<Listener>(1);
		listeners.add(new Listener("HTTP", 80, 80));
		listeners.add(new Listener("HTTP", 8080, 8080));

		lbRequest.withAvailabilityZones("us-east-1b");
		lbRequest.setListeners(listeners);

		CreateLoadBalancerResult lbResult = elb.createLoadBalancer(lbRequest);

		String dns = lbResult.getDNSName();
		System.out.println("created load balancer loader");
		System.out.println("DNS is : " + dns);
		AmazonAutoScalingClient asc = new AmazonAutoScalingClient(bawsc);

		CreateLaunchConfigurationRequest crl = new CreateLaunchConfigurationRequest();
		// InstanceMonitoring i = new InstanceMonitoring();

		crl.setImageId("ami-2b7b2c42");
		crl.setInstanceType("m1.small");
		crl.setInstanceMonitoring((new InstanceMonitoring()).withEnabled(true));
		crl.setLaunchConfigurationName("AnoopLaunchConfiguration17");

		asc.createLaunchConfiguration(crl);
		System.out.println("Configuration has been set");

		UpdateAutoScalingGroupRequest acUpdateRequest = new UpdateAutoScalingGroupRequest();
		acUpdateRequest.setMinSize(0);
		acUpdateRequest.setMaxSize(0);
		acUpdateRequest.setDesiredCapacity(0);
		acUpdateRequest.setAutoScalingGroupName("AnoopAutoScalingGroup13");
		asc.updateAutoScalingGroup(acUpdateRequest);
		System.out.println("Scaling group has been updated");

		CreateAutoScalingGroupRequest acRequest = new CreateAutoScalingGroupRequest();
		acRequest.setMinSize(2);
		acRequest.setMaxSize(5);
		acRequest.setDesiredCapacity(2);
		acRequest.setAutoScalingGroupName("AnoopAutoScalingGroup14");
		acRequest.setLaunchConfigurationName("AnoopLaunchConfiguration17");
		acRequest.setLoadBalancerNames(elbnames);
		acRequest.withAvailabilityZones("us-east-1b");

		asc.createAutoScalingGroup(acRequest);
		System.out.println("Scaling group has been done");

		PutScalingPolicyRequest pscr = new PutScalingPolicyRequest();
		pscr.setPolicyName("AnoopScalingPolicies");
		pscr.setAutoScalingGroupName("AnoopAutoScalingGroup14");

		pscr.setScalingAdjustment(1);
		pscr.setAdjustmentType("ChangeInCapacity");

		pscr.setScalingAdjustment(-1);
		pscr.setAdjustmentType("ChangeInCapacity");

		PutScalingPolicyResult pres = asc.putScalingPolicy(pscr);

		AmazonCloudWatchClient acwc = new AmazonCloudWatchClient(bawsc);

		PutMetricAlarmRequest mreq = new PutMetricAlarmRequest();
		mreq.setAlarmName("AnoopAlarm");
		// mreq.withAlarmName("AlarmAnoop");
		Dimension dimension = new Dimension();
		dimension.withName("AutoScalingGroupName").withValue(
				"AnoopAutoScalingGroup14");
		pres.getPolicyARN();
		// System.out.println("ARN is: " + ARN);
		mreq.withPeriod(60).withEvaluationPeriods(5).withThreshold(80.0)
				.withAlarmName("AnoopAlarm")
				.withAlarmActions(pres.getPolicyARN())
				.withComparisonOperator("GreaterThanThreshold")
				.withAlarmDescription("Scale-up if CPU > 80% for 5 minutes")
				.withMetricName("CPUUtilization").withNamespace("AWS/EC2")
				.withStatistic("Average").withEvaluationPeriods(5)
				.withDimensions(dimension);

		acwc.putMetricAlarm(mreq);

		mreq = new PutMetricAlarmRequest();
		mreq.withPeriod(60).withEvaluationPeriods(5).withThreshold(20.0)
				.withAlarmName("AnoopAlarm")
				.withAlarmActions(pres.getPolicyARN())
				.withComparisonOperator("LessThanThreshold")
				.withAlarmDescription("Scale-down if CPU < 20% for 5 minutes")
				.withMetricName("CPUUtilization").withNamespace("AWS/EC2")
				.withStatistic("Average").withEvaluationPeriods(5)
				.withDimensions(dimension);

		acwc.putMetricAlarm(mreq);

		String topicARN = "arn:aws:sns:us-east-1:474626275331:AnoopTopic1";
		List<String> notificationTypes = new ArrayList<String>();
		notificationTypes.add("autoscaling:EC2_INSTANCE_LAUNCH");
		notificationTypes.add("autoscaling:EC2_INSTANCE_TERMINATE");

		PutNotificationConfigurationRequest pncr = new PutNotificationConfigurationRequest();
		pncr.withAutoScalingGroupName("AnoopAutoScalingGroup14")
				.withTopicARN(topicARN)
				.withNotificationTypes(notificationTypes);

		asc.putNotificationConfiguration(pncr);

		List<Instance> instances;
		do {
			DescribeLoadBalancersRequest dlbreq = new DescribeLoadBalancersRequest();
			// Configure request with specific ELB name
			dlbreq.withLoadBalancerNames(elbnames);
			// Run the request
			DescribeLoadBalancersResult dlbres = elb
					.describeLoadBalancers(dlbreq);
			// Get the Load Balancer Description (ELB with specific name)
			LoadBalancerDescription loadBalancer = dlbres
					.getLoadBalancerDescriptions().get(0);
			// Return all of the ELB's instances
			instances = loadBalancer.getInstances();
			// Loop while there is at least one instance Out Of Service
		} while (instances.size() < 2);

		boolean isReady = false;
		do {
			// Initiate DescribeInstanceHealthRequest to get the ELB Instance
			// status
			DescribeInstanceHealthRequest dihreq = new DescribeInstanceHealthRequest();
			// Configure the requst with specific ELB Name and specific list of
			// ELB Instances
			dihreq.withInstances(instances).withLoadBalancerName(
					elbnames.get(0));
			// Run the request
			DescribeInstanceHealthResult res = elb
					.describeInstanceHealth(dihreq);
			// Iterate in all instances to check if the status is InService
			for (InstanceState instanceState : res.getInstanceStates()) {
				if (!instanceState.getState().equals("InService")) {
					// Break out the look if the status is Out Of Service
					isReady = false;
					break;
				} else {
					isReady = true;
				}
			}
		} while (!isReady);

		JSch jsch = new JSch();
		String host = "ec2-54-225-15-66.compute-1.amazonaws.com";
		String keyFile = "lib/ec2privatekey.pem";
		jsch.addIdentity(keyFile);
		Session session = jsch.getSession("ubuntu", host, 22);
		java.util.Properties config = new java.util.Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		session.connect();

		ChannelExec channelExec = (ChannelExec) session.openChannel("exec");

		// InputStream in = channelExec.getInputStream();

		channelExec.setCommand("cd $HOME/benchmark && sudo ./seige_bench.sh "
				+ dns);
		channelExec.connect();

		InputStream in = channelExec.getInputStream();
		byte[] tmp = new byte[1024];
		while (true) {
			while (in.available() > 0) {
				int b = in.read(tmp, 0, 1024);
				if (b < 0)
					break;
				System.out.print(new String(tmp, 0, b));
			}
			if (channelExec.isClosed()) {
				System.out.println("exit-status: "
						+ channelExec.getExitStatus());
				break;
			}
			try {
				Thread.sleep(1000);
			}

			catch (Exception ee1) {
				System.out.print("Inside medium" + ee1);
			}

		}

		acUpdateRequest = new UpdateAutoScalingGroupRequest();
		acUpdateRequest.setMinSize(0);
		acUpdateRequest.setMaxSize(0);
		acUpdateRequest.setDesiredCapacity(0);
		acUpdateRequest.setAutoScalingGroupName("AnoopAutoScalingGroup14");
		asc.updateAutoScalingGroup(acUpdateRequest);

		Thread.sleep(120000);

		isReady = false;
		do {
			DescribeScalingActivitiesRequest dsar = new DescribeScalingActivitiesRequest();
			dsar.withAutoScalingGroupName("AnoopAutoScalingGroup14")
					.withMaxRecords(1);

			DescribeScalingActivitiesResult dsares = asc
					.describeScalingActivities(dsar);

			if (dsares.getActivities().get(0).getStatusCode()
					.equals("InProgress")) {
				isReady = true;
			}

		} while (!isReady);
		
		DeleteAutoScalingGroupRequest dasgr = new DeleteAutoScalingGroupRequest();
		dasgr.withAutoScalingGroupName("AnoopAutoScalingGroup14");
		
		asc.deleteAutoScalingGroup(dasgr);
		
		DeleteLaunchConfigurationRequest dlcr =new DeleteLaunchConfigurationRequest();
		dlcr.withLaunchConfigurationName("AnoopLaunchConfiguration17");
		
		asc.deleteLaunchConfiguration(dlcr);
		
		DeleteLoadBalancerRequest dlbr = new DeleteLoadBalancerRequest(elbnames.get(0));
		elb.deleteLoadBalancer(dlbr);

	}
}