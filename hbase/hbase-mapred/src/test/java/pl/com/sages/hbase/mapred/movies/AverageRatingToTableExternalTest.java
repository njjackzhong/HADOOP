package pl.com.sages.hbase.mapred.movies;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Job;
import org.junit.Before;
import org.junit.Test;
import pl.com.sages.hbase.api.dao.RatingDao;
import pl.com.sages.hbase.api.util.ConnectionHandler;
import pl.com.sages.hbase.api.util.HBaseUtil;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class AverageRatingToTableExternalTest {

    private static final TableName TABLE_NAME = HBaseUtil.getUserTableName("ratings_average");
    private static final String FAMILY_NAME = "ratings_average";
    private static final String QUALIFIER_NAME = "average";

    @Before
    public void before() throws IOException {
        HBaseUtil.recreateTable(TABLE_NAME, FAMILY_NAME);
    }

    @Test
    public void shouldRunMapReduce() throws Exception {
        //given
        Configuration configuration = HBaseConfiguration.create();
        configuration.set(AverageRatingReducer.FAMILY, FAMILY_NAME);
        configuration.set(AverageRatingReducer.QUALIFIER, QUALIFIER_NAME);

        Job job = Job.getInstance(configuration, "Average Rating");
        job.setJarByClass(AverageRatingMapper.class);

        Scan scan = new Scan();
        scan.setCaching(500);
        scan.setCacheBlocks(false);
        scan.addFamily(RatingDao.CF);

        TableMapReduceUtil.initTableMapperJob(
                RatingDao.TABLE,
                scan,
                AverageRatingMapper.class,
                IntWritable.class,
                DoubleWritable.class,
                job);
        TableMapReduceUtil.initTableReducerJob(
                TABLE_NAME.getNameAsString(),
                AverageRatingReducer.class,
                job);

        //when
        boolean succeeded = job.waitForCompletion(true);

        //then
        Connection connection = ConnectionHandler.getConnection();

        assertThat(succeeded).isTrue();
        assertThat(connection.getAdmin().tableExists(TABLE_NAME)).isTrue();

        Table table = connection.getTable(TABLE_NAME);
        scan = new Scan();
        int count = 0;
        ResultScanner results = table.getScanner(scan);
        for (Result result : results) {
            count++;
        }
        table.close();
        assertThat(count).isGreaterThan(1000);
    }

}