import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.AmazonS3Exception
import spock.lang.Specification

class AwsAccessInvalid extends Specification {



    def "Invalid access key" (){

        given: "Connection made"
        AWSCredentials credentials = new BasicAWSCredentials(
                "aaaaaaaaa",  //keys not supplied in public repo
                "aaaaaaaaaaaaaaaaaaaaaaaaaaa"
        )
        AmazonS3 s3client = new AmazonS3Client(credentials);

        when: "Attempt to upload file"
        s3client.putObject("dhill-test-automation-in/input/", "valid.csv", new File("src/test/resources/valid.csv"))

        then: "AmazonS3Exception is thrown"
        def ex = thrown(AmazonS3Exception.class)
        assert ex.getMessage().contains("The AWS Access Key Id you provided does not exist in our records")
    }

    def "Invalid secret key" (){

        given: "Connection made"
        AWSCredentials credentials = new BasicAWSCredentials(
                "aaaaaaaaa",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaa"
        )
        AmazonS3 s3client = new AmazonS3Client(credentials);

        when: "Attempt to upload file"
        s3client.putObject("dhill-test-automation-in/input/", "valid.csv", new File("src/test/resources/valid.csv"))

        then: "AmazonS3Exception is thrown"
        def ex = thrown(AmazonS3Exception.class)
        assert ex.getMessage().contains("The request signature we calculated does not match the signature you provided")
    }



}
