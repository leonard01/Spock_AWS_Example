import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import org.apache.commons.io.IOUtils
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser

class S3Connect {

    AmazonS3 s3client

    /**
     * connect to S3 client
     */
    S3Connect(){

        AWSCredentials credentials = new BasicAWSCredentials(
                "AKIA2YVRLFYRHVM6J2SY",
                "1j+7HcmFcvUD3hxKeAy40MqW48nm2KIywebnh6l2"
        );
        s3client = new AmazonS3Client(credentials);
    }

    /**
     * Consumes files in bucket, adding to input stream then writing to JSON
     * @return JSON object
     */
    def getJsonFromS3File(){

        sleep(5000)
        ObjectListing objectListing = s3client.listObjects("dhill-test-automation-out")
        InputStream objectData
        for(S3ObjectSummary os : objectListing.getObjectSummaries()) {
            S3Object s3Object = s3client.getObject("dhill-test-automation-out", os.getKey())
            objectData = s3Object.getObjectContent()
        }
        String jsonTxt = IOUtils.toString(objectData, "UTF-8");
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(jsonTxt)
        return json
    }
}
