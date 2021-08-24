import com.amazonaws.services.s3.model.Bucket
import com.amazonaws.services.s3.model.ObjectListing
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.services.s3.model.S3ObjectSummary
import spock.lang.Shared
import spock.lang.Specification



class AwsAccess extends Specification {

    @Shared S3Connect s3 = new S3Connect()

    def cleanup(){

        ObjectListing objectListing = s3.s3client.listObjects("dhill-test-automation-out")
        for(S3ObjectSummary os : objectListing.getObjectSummaries()) {
            s3.s3client.deleteObject("dhill-test-automation-out", os.getKey());
        }
    }

    def "List Buckets" (){

        when: "Connection made and buckets retrieved"
         def bucketList = []
         for (Bucket bucket : s3.s3client.listBuckets()) {
                bucketList << new Bucket(bucket.getName())
                println(" - " + bucket.getName());
         }

        then: "Buckets Checked"
        assert bucketList.toString().contains("[name=dhill-test-automation-in, creationDate=null, owner=null], S3Bucket [name=dhill-test-automation-out, creationDate=null, owner=null]")
     }


    def "Upload Valid File" (){

        when: "Connection made and valid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "valid.csv", new File("src/test/resources/valid.csv"))

        then: "Download file from input bucket and compare"
        def result = s3.getJsonFromS3File()
        assert result.Records.Record.Transaction[0].toString().contains("{\"Status\":\"PAID\",\"VAT Number\":\"N\\/A\",\"Price\":\"499.99\",\"Date\":\"2009-01-21T16:21:32\"}")
        assert result.Records.Record.Transaction[1].toString().contains("{\"Status\":\"UNPAID\",\"VAT Number\":\"IE2132763A\",\"Price\":\"449.99\",\"Date\":\"2021-01-21T12:59:11\"}")
    }

    def "Upload Valid 1 row File" (){

        when: "Connection made and valid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "valid1row.csv", new File("src/test/resources/valid1row.csv"))

        then: "Download file from input bucket and compare"
        def result = s3.getJsonFromS3File()
        assert result.Records.Record.Transaction[0].toString().contains("{\"Status\":\"PAID\",\"VAT Number\":\"N\\/A\",\"Price\":\"499.99\",\"Date\":\"2009-01-21T16:21:32\"}")
    }

    //Sleep inside loop required as lambda function seems to hit issues when dealing with queued files to process
    //Test will fail as connection fails with timeout
    def "Upload 100s of Files" (){

        when: "Connection made and valid csv file uploaded"
        int numberOfFilesUpload = 100
        for(int i = 0; i < numberOfFilesUpload; i++) {
            s3.s3client.putObject("dhill-test-automation-in/input/", "valid1row.csv", new File("src/test/resources/valid1row.csv"))
            sleep(500)
        }

        then: "Download file from input bucket and compare"
        int numberofFilesCount
        ObjectListing objectListing = s3.s3client.listObjects("dhill-test-automation-out")
        for(S3ObjectSummary os : objectListing.getObjectSummaries()) {
            S3Object s3Object = s3.s3client.getObject("dhill-test-automation-out", os.getKey())
            numberofFilesCount++
        }
        println(numberOfFilesUpload)
        assert numberOfFilesUpload == numberofFilesCount
    }

    def "Upload Valid Transaction (processing) File" (){

        when: "Connection made and valid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "valid-transaction1.csv", new File("src/test/resources/valid-transaction1.csv"))

        then: "Download file from input bucket and compare"
        def result = s3.getJsonFromS3File()
        assert result.Records.Record.Transaction[0].toString().contains("{\"Status\":\"Processing\",\"VAT Number\":\"N\\/A\",\"Price\":\"499.99\",\"Date\":\"2009-01-21T16:21:32\"}")
   }


    def "Upload Large Valid File" (){

        when: "Connection made and valid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "largefile.csv", new File("src/test/resources/largefile.csv"))

        then: "Download file from input bucket and compare"
        String outputFromFile = s3.getJsonFromS3File().get("Records").toString()
        String findStr = "Darren Rabbitt"
        int lastIndex = 0;
        int count = 0;

        while(lastIndex != -1){
            lastIndex = outputFromFile.indexOf(findStr,lastIndex);
            if(lastIndex != -1){
                count ++;
                lastIndex += findStr.length()
            }
        }
        assert count == 152
        //we should not rely on hard coded count - a method to read the local csv file and return number to compare is needed
    }


    def "Upload invalid id wrong data type char File" (){

        when: "Connection made and invalid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "invalidid_char.csv", new File("src/test/resources/invalidid_char.csv"))

        then: "Download file from input bucket and compare"
        def result = s3.getJsonFromS3File()
        assert result.Records.Error[0].toString().contains("{\"ErrorCode\":\"E2\",\"ErrorMessage\":\"ID field is empty, malformed or over 10 digits long\"}")

    }


    def "Upload invalid id no of chars File" (){

        when: "Connection made and invalid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "invalidid_NoOfChar.csv", new File("src/test/resources/invalidid_NoOfChar.csv"))

        then: "Download file from input bucket and compare"
        def result = s3.getJsonFromS3File()
        assert result.Records.Error[0].toString().contains("{\"ErrorCode\":\"E2\",\"ErrorMessage\":\"ID field is empty, malformed or over 10 digits long\"}")
    }


    def "Upload invalid product File" (){

        when: "Connection made and invalid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "invalidproduct.csv", new File("src/test/resources/invalidproduct.csv"))

        then: "Download file from input bucket and compare"
        def result = s3.getJsonFromS3File()
        assert result.Records.Error[0].toString().contains("{\"ErrorCode\":\"E3\",\"ErrorMessage\":\"Product field is empty or over 256 characters long\"}")
    }


    def "Upload invalid vendor File" (){

        when: "Connection made and invalid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "invalidvendor.csv", new File("src/test/resources/invalidvendor.csv"))

        then: "Download file from input bucket and compare"
        def result = s3.getJsonFromS3File()
        assert result.Records.Error[0].toString().contains("{\"ErrorCode\":\"E4\",\"ErrorMessage\":\"Vendor field is empty or over 256 characters long\"}")
    }


    def "Upload invalid transaction date File" (){

        when: "Connection made and invalid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "invalidTD.csv", new File("src/test/resources/invalidTD.csv"))

        then: "Download file from input bucket and compare"
        def result = s3.getJsonFromS3File()
        assert result.Records.Error[0].toString().contains("{\"ErrorCode\":\"E5\",\"ErrorMessage\":\"Date field needs to be in this format - YYYY-MM-DDTHH:MM:SS\"}")
    }

    def "Upload invalid transaction price File" (){

        when: "Connection made and invalid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "invalidprice.csv", new File("src/test/resources/invalidprice.csv"))

        then: "Download file from input bucket and compare"
        def result = s3.getJsonFromS3File()
        assert result.Records.Error[0].toString().contains("{\"ErrorCode\":\"E6\",\"ErrorMessage\":\"Price field is malformed\"}")

        assert s3.getJsonFromS3File().get("Records").toString().contains("{\"Error\":{\"ErrorCode\":\"E6\",\"ErrorMessage\":\"Price field is malformed\"}")
    }



    def "Upload invalid transaction status File" (){

        when: "Connection made and invalid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "invalidTS.csv", new File("src/test/resources/invalidTS.csv"))

        then: "Download file from input bucket and compare"
        def result = s3.getJsonFromS3File()
        assert result.Records.Error[0].toString().contains("{\"ErrorCode\":\"E7\",\"ErrorMessage\":\"Transaction Status needs to be either 'Paid', 'Unpaid' or 'Processing'\"}")
    }


    //This test will fail as there is no validation on VAT number
    def "Upload invalid VAT number status File" (){

        when: "Connection made and invalid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "invalidVN.csv", new File("src/test/resources/invalidVN.csv"))

        then: "Download file from input bucket and compare"
        String vat_number_error = ""
        assert s3.getJsonFromS3File().get("Records").toString().contains("{\"Error\":{\"ErrorCode\":\"E7\",\"ErrorMessage\":\"VAT Number $vat_number_error}'\"}")
    }


    def "Upload invalid customer status File" (){

        when: "Connection made and invalid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "invalidcustomer.csv", new File("src/test/resources/invalidcustomer.csv"))

        then: "Download file from input bucket and compare"
        def result = s3.getJsonFromS3File()
        assert result.Records.Error[0].toString().contains("{\"ErrorCode\":\"E9\",\"ErrorMessage\":\"Customer field is empty or over 256 characters long\"}")
     }


    def "Upload too few rows File" (){

        when: "Connection made and invalid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "toofewfields.csv", new File("src/test/resources/toofewfields.csv"))

        then: "Download file from input bucket and compare"
        def result = s3.getJsonFromS3File()
        assert result.Records.Error[0].toString().contains("{\"ErrorCode\":\"E1\",\"ErrorMessage\":\"Incorrect number of fields in record\"}")
    }

    def "Upload too many rows File" (){

        when: "Connection made and invalid csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "toomanyfields.csv", new File("src/test/resources/toomanyfields.csv"))

        then: "Download file from input bucket and compare"
        def result = s3.getJsonFromS3File()
        assert result.Records.Error[0].toString().contains("{\"ErrorCode\":\"E1\",\"ErrorMessage\":\"Incorrect number of fields in record\"}")
  }


    def "Upload empty csv File" (){

        when: "Connection made and empty csv file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "empty.csv", new File("src/test/resources/empty.csv"))

        then: "Files Checked"
        sleep(5000)
        ObjectListing objectListing = s3.s3client.listObjects("dhill-test-automation-out")
        S3Object s3Object
        for(S3ObjectSummary os : objectListing.getObjectSummaries()) {
             s3Object = s3.s3client.getObject("dhill-test-automation-out", os.getKey())

        }
        assert s3Object.key.contains("-F2")
    }


    def "Upload txt file" (){

        when: "Connection made and invalid txt file uploaded"
        s3.s3client.putObject("dhill-test-automation-in/input/", "invalid.txt", new File("src/test/resources/invalid.txt"))
        then: "Check for that files have not been processed and sent to outbucket"
        sleep(5000)
        ObjectListing objectListing = s3.s3client.listObjects("dhill-test-automation-out")
        for(S3ObjectSummary os : objectListing.getObjectSummaries()) {
            S3Object s3Object = s3.s3client.getObject("dhill-test-automation-out", os.getKey())
         assert s3Object == null
        }
  }







}
