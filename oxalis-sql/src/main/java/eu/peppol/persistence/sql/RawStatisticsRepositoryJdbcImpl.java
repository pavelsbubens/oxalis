package eu.peppol.persistence.sql;

import eu.peppol.persistence.sql.dao.DimensionJdbcHelper;
import eu.peppol.persistence.sql.util.DataSourceHelper;
import eu.peppol.persistence.sql.util.JdbcHelper;
import eu.peppol.persistence.sql.util.JdbcScriptRunner;
import eu.peppol.statistics.*;

import javax.sql.DataSource;
import java.io.*;
import java.sql.*;
import java.util.Calendar;
import java.util.Date;

/**
 * JDBC implementation of StatisticsRepository component supplied with Oxalis. In theory, you may use any implementation of
 * StatisticsRepository you like, however; in real life, most people will probably stick with the SQL database.
 * <p/>
 * Henceforth this implementation is located here in the commons component of Oxalis, in order to be used by either
 * the DBCP or the JNDI implementation of StatisticsRepository.
 * <p/>
 *
 * User: steinar
 * Date: 30.01.13
 * Time: 19:32
 */
public class RawStatisticsRepositoryJdbcImpl implements RawStatisticsRepository {

    public static final String RAW_STATS_TABLE_NAME = "raw_stats";
    private final DataSourceHelper dataSourceHelper;

    public RawStatisticsRepositoryJdbcImpl(DataSource dataSource) {
        dataSourceHelper = new DataSourceHelper(dataSource);
    }


    /**
     * Persists raw statistics into the DBMS via JDBC, no caching is utilized.
     *
     * @param rawStatistics
     * @return
     */
    @Override
    public Integer persist(RawStatistics rawStatistics) {
        Connection con = null;
        PreparedStatement ps;

        Integer result = 0;

        try {

            con = dataSourceHelper.getConnectionWithAutoCommit();

            String sqlStatement = String.format("INSERT INTO %s (ap, tstamp,  direction, sender, receiver, doc_type, profile, channel) values(?,?,?,?,?,?,?,?)", RAW_STATS_TABLE_NAME);
            ps = con.prepareStatement(sqlStatement, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, rawStatistics.getAccessPointIdentifier().toString());
            ps.setTimestamp(2, new Timestamp(rawStatistics.getDate().getTime()));
            ps.setString(3, rawStatistics.getDirection().toString());
            ps.setString(4, rawStatistics.getSender().stringValue());
            ps.setString(5, rawStatistics.getReceiver().stringValue());
            ps.setString(6, rawStatistics.getPeppolDocumentTypeId().toString());
            ps.setString(7, rawStatistics.getPeppolProcessTypeId().toString());
            ps.setString(8, rawStatistics.getChannelId() == null ? null : rawStatistics.getChannelId().stringValue());

            int rc = ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                result = rs.getInt(1);
                rs.close();
            }

        } catch (SQLException e) {
            throw new IllegalStateException("Unable to execute statement " + e, e);
        } finally {
            DataSourceHelper.close(con);
        }
        return result;
    }






    @Override
    public void fetchAndTransformRawStatistics(StatisticsTransformer transformer, Date start, Date end, StatisticsGranularity granularity) {

        String sql = SQLComposer.createRawStatisticsSqlQueryText(granularity);

        start = JdbcHelper.setStartDateIfNull(start);
        end = JdbcHelper.setEndDateIfNull(end);

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = dataSourceHelper.getConnectionWithAutoCommit();
            ps = con.prepareStatement(sql);

            // Sets the start and end parameters for both parts of the SELECT UNION
            ps.setTimestamp(1, new java.sql.Timestamp(start.getTime()));
            ps.setTimestamp(2, new Timestamp(end.getTime()));
            ps.setTimestamp(3, new Timestamp(start.getTime()));
            ps.setTimestamp(4, new Timestamp(end.getTime()));
            ResultSet rs = ps.executeQuery();

            transformer.startStatistics(start,end);
            while (rs.next()) {
                transformer.startEntry();
                transformer.writeAccessPointIdentifier(rs.getString("ap"));
                transformer.writeDirection(rs.getString("direction"));
                transformer.writePeriod(rs.getString("period"));
                transformer.writeParticipantIdentifier(rs.getString("ppid"));
                transformer.writeDocumentType(rs.getString("doc_type"));
                transformer.writeProfileId(rs.getString("profile"));
                transformer.writeChannel(rs.getString("channel"));
                transformer.writeCount(rs.getInt("count"));
                transformer.endEntry();
            }
            transformer.endStatistics();
        } catch (SQLException e) {
            throw new IllegalStateException("SQL error:" + e, e);
        } finally {
            DataSourceHelper.close(con);
        }
    }





}
