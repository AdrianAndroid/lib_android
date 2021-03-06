// Tencent is pleased to support the open source community by making Mars available.
// Copyright (C) 2017 THL A29 Limited, a Tencent company. All rights reserved.

// Licensed under the MIT License (the "License"); you may not use this file except in 
// compliance with the License. You may obtain a copy of the License at
// http://opensource.org/licenses/MIT

// Unless required by applicable law or agreed to in writing, software distributed under the License is
// distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
// either express or implied. See the License for the specific language governing permissions and
// limitations under the License.

/*
*  ChatCGITask.cpp
*
*  Created on: 2017-7-7
*      Author: chenzihao
*/

#include <mars/comm/windows/projdef.h>
#include "ChatCGITask.h"
#include "proto/generate/chat.pb.h"
#include "mars/stn/stn_logic.h"
using namespace std;

bool ChatCGITask::Req2Buf(uint32_t _taskid, void* const _user_context, AutoBuffer& _outbuffer, AutoBuffer& _extend, int& _error_code, const int _channel_select)
{
	string data;
	com::tencent::mars::sample::chat::proto::SendMessageRequest request;
	request.set_from(user_);
	request.set_to(to_);
	request.set_access_token(access_token_);
	request.set_topic(topic_);
	request.set_text(text_);
	request.SerializeToString(&data);
	_outbuffer.AllocWrite(data.size());
	_outbuffer.Write(data.c_str(), data.size());
	return true;
}
int ChatCGITask::Buf2Resp(uint32_t _taskid, void* const _user_context, const AutoBuffer& _inbuffer, const AutoBuffer& _extend, int& _error_code, const int _channel_select)
{
	com::tencent::mars::sample::chat::proto::SendMessageResponse response;
	response.ParseFromArray(_inbuffer.Ptr(), _inbuffer.Length());
	return mars::stn::kTaskFailHandleNoError;
}

