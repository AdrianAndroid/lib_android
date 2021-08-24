// Tencent is pleased to support the open source community by making Mars available.
// Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.

// Licensed under the MIT License (the "License"); you may not use this file except in 
// compliance with the License. You may obtain a copy of the License at
// http://opensource.org/licenses/MIT

// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.


/*
 * log_formater.cpp
 *
 *  Created on: 2013-3-8
 *      Author: yerungui
 */


#include <assert.h>
#include <stdio.h>
#include <limits.h>
#include <algorithm>
#include <string>

#include "mars/comm/xlogger/xloggerbase.h"
#include "mars/comm/xlogger/loginfo_extract.h"
#include "mars/comm/ptrbuffer.h"

#ifdef _WIN32
#define PRIdMAX "lld"
#define snprintf _snprintf
#else
#define __STDC_FORMAT_MACROS
#include <inttypes.h>
#endif

namespace mars {
namespace xlog {

#if !defined(ANDROID) && !_WIN32
static char* reverse(char *str, int len) {
    char* p1 = str;
    char* p2 = str + len - 1;

    while (p1 < p2) {
        char tmp = *p1;
        *p1++ = *p2;
        *p2-- = tmp;
    }

    return str;
}

static int logger_itoa(int num, char* str, int len, int min) {
    if (NULL == str || len == 0) {
        return 0;
    }

    int sign = num;
    if (sign < 0) {
        num = -num;
        len--;
        min--;
    }

    int i = 0;
    do {
        str[i++] = '0' + num % 10;
        num /= 10;
    } while (num && (i < (len - 1)));

    while (i < min && i < (len - 1)) {
        str[i++] = '0';
    }
    if (sign < 0) {
        str[i++] = '-';
    }
    str[i] = '\0';
    reverse(str, i);
    return i;
}

void format_time(char buffer[64], time_t _seconds, suseconds_t _microseconds) {
    thread_local time_t init_seconds = 0;
    thread_local struct tm tm;
    thread_local int gmtoff = tm.tm_gmtoff / 360;

    static const uint64_t kInterval = 30 * 60;

    if (0 == init_seconds || _seconds < init_seconds || (_seconds - init_seconds) > kInterval) {
        init_seconds = _seconds;
        memset(&tm, 0, sizeof(tm));
        localtime_r((const time_t*)&_seconds, &tm);
        gmtoff = tm.tm_gmtoff / 360;
    }

    int year = 1900 + tm.tm_year;
    int mon = 1 + tm.tm_mon;
    int day = tm.tm_mday;
    int hour = tm.tm_hour;
    int min = tm.tm_min;
    int sec = tm.tm_sec + (_seconds - init_seconds);
    int msec = _microseconds / 1000;

    do {
        if (sec < 60) {
            break;
        }
        int cnt = sec / 60;
        sec %= 60;
        min += cnt;
        if (min < 60) {
            break;
        }
        cnt = min / 60;
        min %= 60;
        hour += cnt;
        if (hour < 24) {
            break;
        }
        cnt = hour / 24;
        hour %= 24;
        int base_day = 0;
        if (1 == mon || 3 == mon || 5 == mon || 7 == mon || 8 == mon || 10 == mon || 12 == mon) {
            base_day = 31;
        } else if (2 == mon) {
            if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) {
                base_day = 29;
            } else {
                base_day = 28;
            }
        } else {
            base_day = 30;
        }
        day += cnt;
        if (day < base_day + 1) {
            break;
        }
        day -= base_day;
        if (++mon < 13) {
            break;
        }
        mon -=12;
        year++;
    } while (false);
    // snprintf(buffer, 64, "%d-%02d-%02d +%.1f %02d:%02d:%02d.%.3d", year, mon, day, gmtoff, hour, min, sec, msec);

    do {
        int len = 0;
        int total_len = 64;
        len += logger_itoa(year, buffer + len, total_len - len, 4);
        if (len >= total_len - 1) {
            break;
        }
        buffer[len++] = '-';
        len += logger_itoa(mon, buffer + len, total_len - len, 2);
        if (len >= total_len - 1) {
            break;
        }
        buffer[len++] = '-';
        len += logger_itoa(day, buffer + len, total_len - len, 2);
        if (len >= total_len - 2) {
            break;
        }
        buffer[len++] = ' ';
        if (gmtoff > 0) {
            buffer[len++] = '+';
        }
        len += logger_itoa(gmtoff, buffer + len, total_len - len, 0);
        if (len >= total_len - 1) {
            break;
        }
        buffer[len++] = ' ';
        len += logger_itoa(hour, buffer + len, total_len - len, 2);
        if (len >= total_len - 1) {
            break;
        }
        buffer[len++] = ':';
        len += logger_itoa(min, buffer + len, total_len - len, 2);
        if (len >= total_len - 1) {
            break;
        }
        buffer[len++] = ':';
        len += logger_itoa(sec, buffer + len, total_len - len, 2);
        if (len >= total_len - 1) {
            break;
        }
        buffer[len++] = '.';
        len += logger_itoa(msec, buffer + len, total_len - len, 3);
    } while (false);

}
#endif

void log_formater(const XLoggerInfo* _info, const char* _logbody, PtrBuffer& _log) {
    static const char* levelStrings[] = {
        "V",
        "D",  // debug
        "I",  // info
        "W",  // warn
        "E",  // error
        "F"  // fatal
    };

    assert((unsigned int)_log.Pos() == _log.Length());

    static int error_count = 0;
    static int error_size = 0;

    if (_log.MaxLength() <= _log.Length() + 5 * 1024) {  // allowed len(_log) <= 11K(16K - 5K)
        ++error_count;
        error_size = (int)strnlen(_logbody, 1024 * 1024);

        if (_log.MaxLength() >= _log.Length() + 128) {
            int ret = snprintf((char*)_log.PosPtr(), 1024, "[F]log_size <= 5*1024, err(%d, %d)\n", error_count, error_size);  // **CPPLINT SKIP**
            _log.Length(_log.Pos() + ret, _log.Length() + ret);
            _log.Write("");

            error_count = 0;
            error_size = 0;
        }

        assert(false);
        return;
    }

    if (NULL != _info) {
        const char* filename = ExtractFileName(_info->filename);

#if _WIN32
        char strFuncName [128] = {0};
        ExtractFunctionName(_info->func_name, strFuncName, sizeof(strFuncName));
#else
        const char* strFuncName = NULL == _info->func_name ? "" : _info->func_name;
#endif

        char temp_time[64] = {0};

        if (0 != _info->timeval.tv_sec) {
#if defined(ANDROID) || _WIN32
            time_t sec = _info->timeval.tv_sec;
            struct tm tm;
            memset(&tm, 0, sizeof(tm));
#ifdef _WIN32
#else
            localtime_r((const time_t*)&sec, &tm);
            std::string gmt = std::to_string(tm.tm_gmtoff / 360);
#endif
#endif
            
#ifdef ANDROID
            snprintf(temp_time, sizeof(temp_time), "%d-%02d-%02d +%.3s %02d:%02d:%02d.%.3ld", 1900 + tm.tm_year, 1 + tm.tm_mon, tm.tm_mday,
                     gmt.c_str(), tm.tm_hour, tm.tm_min, tm.tm_sec, _info->timeval.tv_usec / 1000);
#elif _WIN32
            snprintf(temp_time, sizeof(temp_time), "%d-%02d-%02d +%.3s %02d:%02d:%02d.%.3d", 1900 + tm.tm_year, 1 + tm.tm_mon, tm.tm_mday,
                     (-_timezone) / 3600.0, tm.tm_hour, tm.tm_min, tm.tm_sec, _info->timeval.tv_usec / 1000);
#else
            format_time(temp_time, _info->timeval.tv_sec, _info->timeval.tv_usec);
#endif
        }

        // _log.AllocWrite(30*1024, false);
#if defined(ANDROID) || _WIN32
        int len = snprintf((char*)_log.PosPtr(), 1024, "[%s][%s][%" PRIdMAX ", %" PRIdMAX "%s][%s][%s, %s, %d][",  // **CPPLINT SKIP**
                           _logbody ? levelStrings[_info->level] : levelStrings[kLevelFatal], temp_time,
                           _info->pid, _info->tid, _info->tid == _info->maintid ? "*" : "", _info->tag ? _info->tag : "",
                           filename, strFuncName, _info->line);
#else
        int len = 0;
        int max_len = 1024;
        memcpy(_log.PosPtr(), "[", 1);
        len += 1;
        const char* level_str = _logbody ? levelStrings[_info->level] : levelStrings[kLevelFatal];
        size_t level_str_len = strlen(level_str);
        memcpy((char*)_log.PosPtr() + len, level_str, level_str_len);
        len += level_str_len;
        memcpy((char*)_log.PosPtr() + len, "][", 2);
        len += 2;
        int temp_time_len = strnlen(temp_time, sizeof(temp_time));
        memcpy((char*)_log.PosPtr() + len, temp_time, temp_time_len);
        len += temp_time_len;
        memcpy((char*)_log.PosPtr() + len, "][", 2);
        len += 2;
        len += logger_itoa((int)_info->pid, (char*)_log.PosPtr() + len, max_len - len, 0);
        memcpy((char*)_log.PosPtr() + len, ", ", 2);
        len += 2;
        len += logger_itoa((int)_info->tid, (char*)_log.PosPtr() + len, max_len - len, 0);
        if (_info->tid == _info->maintid) {
            memcpy((char*)_log.PosPtr() + len, "*", 1);
            len += 1; 
        }
        memcpy((char*)_log.PosPtr() + len, "][", 2);
        len += 2;
        if (_info->tag) {
            size_t tag_len = strnlen(_info->tag, 100);
            memcpy((char*)_log.PosPtr() + len, _info->tag, tag_len);
            len += tag_len;
        }
        memcpy((char*)_log.PosPtr() + len, "][", 2);
        len += 2;

        size_t filename_len = strnlen(filename, 100);
        memcpy((char*)_log.PosPtr() + len, filename, filename_len);
        len += filename_len; 
        memcpy((char*)_log.PosPtr() + len, ", ", 2);
        len += 2; 

        size_t funname_len = strnlen(strFuncName, 100);
        memcpy((char*)_log.PosPtr() + len, strFuncName, funname_len);
        len += funname_len; 
        memcpy((char*)_log.PosPtr() + len, ", ", 2);
        len += 2; 
        len += logger_itoa(_info->line, (char*)_log.PosPtr() + len, max_len - len, 0);
        memcpy((char*)_log.PosPtr() + len, "][", 2);
        len += 2;
#endif
        assert(0 <= len);
        _log.Length(_log.Pos() + len, _log.Length() + len);
        //      memcpy((char*)_log.PosPtr() + 1, "\0", 1);

        assert((unsigned int)_log.Pos() == _log.Length());
    }

    if (NULL != _logbody) {
        // in android 64bit, in strnlen memchr,  const unsigned char*  end = p + n;  > 4G!!!!! in stack array

        size_t bodylen =  _log.MaxLength() - _log.Length() > 130 ? _log.MaxLength() - _log.Length() - 130 : 0;
        bodylen = bodylen > 0xFFFFU ? 0xFFFFU : bodylen;
        bodylen = strnlen(_logbody, bodylen);
        bodylen = bodylen > 0xFFFFU ? 0xFFFFU : bodylen;
        _log.Write(_logbody, bodylen);
    } else {
        _log.Write("error!! NULL==_logbody");
    }

    char nextline = '\n';

    if (*((char*)_log.PosPtr() - 1) != nextline) _log.Write(&nextline, 1);
}

}
}