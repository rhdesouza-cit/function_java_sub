package org.bullla.poc.sub;

import feign.RequestLine;

public interface HolidayClient {

    @RequestLine("POST /v1/json-server/posts")
    void postHoliday(Holiday holiday);

}