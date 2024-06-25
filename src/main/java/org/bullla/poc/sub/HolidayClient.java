package org.bullla.poc.sub;

import feign.RequestLine;

public interface HolidayClient {

    @RequestLine("POST /messages-holiday-posts")
    void postHoliday(Holiday holiday);

}