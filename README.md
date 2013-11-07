Autoscaling-using-AWS-java-API
==============================
Autoscaling is a service designed by Amazon Web Services to increase or decrease the EC2 instances as per the need. Autoscaling can be done by both Command Line Interface(CLI) and using aws API.

Autoscaling uses Amazon Elastic Load Balancer to check if it needs to increase or decrease the capacity. Moreover, we can set alarm to limit the maximum and minimum capacity.

Moreover, it does the health check to see if all the instances are working well, and if they do not, it just create a new instance and terminate the unhealthy one.
