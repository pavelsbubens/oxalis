/*
 * Copyright (c) 2011,2012,2013 UNIT4 Agresso AS.
 *
 * This file is part of Oxalis.
 *
 * Oxalis is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Oxalis is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Oxalis.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.peppol.inbound.server;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.mail.smime.SMIMESignedParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 * @author steinar
 *         Date: 20.06.13
 *         Time: 00:32
 */
public class AS2Servlet extends HttpServlet {

    public static final Logger log = LoggerFactory.getLogger(AS2Servlet.class);

    protected void doPost(final HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Prepare the MimeMessage stuff...
        Properties properties = System.getProperties();
        Session session = Session.getDefaultInstance(properties, null);

        try {
            // Parses the HTTP input stream into a MimeMessage..
            MimeMessage mimeMessage = new MimeMessage(session, request.getInputStream());
            log.debug("Received MimeMessage: " + mimeMessage.getContentType());

            // Should always be signed
            if (mimeMessage.isMimeType("multipart/signed")) {
                // Parses the multipart signed into the content and the signature...
                SMIMESignedParser smimeSignedParser = new SMIMESignedParser((MimeMultipart) mimeMessage.getContent());

                // Dumps the payload into a file.
                MimeBodyPart content = smimeSignedParser.getContent();
                FileOutputStream fileOutputStream = new FileOutputStream("/tmp/as2dump.xml");
                InputStream inputStream = content.getInputStream();
                int i =0;
                while ((i=inputStream.read()) != -1){
                    fileOutputStream.write(i);
                }
                fileOutputStream.close();

            }
        } catch (Exception e) {
            log.error("Unable to parse SMIME message " + e,e);
            throw new IllegalStateException("Unable to parse SMIME signed message" + e.getMessage(), e);
        }
//        dumpData(request);
    }

    private void dumpData(HttpServletRequest request) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream("/tmp/as2dump.txt");
        ServletInputStream inputStream = request.getInputStream();
        int i = 0;
        while ((i=inputStream.read()) != -1){
            fileOutputStream.write(i);
        }
        fileOutputStream.close();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    }
}
