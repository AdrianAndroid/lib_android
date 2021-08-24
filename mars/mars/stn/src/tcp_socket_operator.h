//
// Created by astro zhou on 2020/7/30.
//

#ifndef MMNET_TCPSOCKETOPERATOR_H
#define MMNET_TCPSOCKETOPERATOR_H

#include "socket_operator.h"
#include <memory>

#include "comm/socket/socketbreaker.h"
#include "comm/socket/complexconnect.h"
#include "mars/stn/stn.h"

namespace mars {
	namespace stn {

class TcpSocketOperator : public SocketOperator {
public:
	TcpSocketOperator(std::shared_ptr<comm::MComplexConnect> _observer);
	virtual ~TcpSocketOperator() override{
	}

	virtual SOCKET Connect(const std::vector<socket_address> &_vecaddr,
	                       mars::comm::ProxyType _proxy_type = mars::comm::kProxyNone,
	                       const socket_address *_proxy_addr = NULL,
	                       const std::string &_proxy_username = "", const std::string &_proxy_pwd = "") override;
    
    virtual void Close(SOCKET _sock) override;
    SocketCloseFunc GetCloseFunction() const override{
        return &socket_close;
    }
    CreateStreamFunc GetCreateStreamFunc() const override{
        return nullptr;
    }
    IsSubStreamFunc GetIsSubStreamFunc() const override{
        return nullptr;
    }
	virtual int
	Send(SOCKET _sock, const void *_buffer, size_t _len, int &_errcode, int _timeout) override;

	virtual int
	Recv(SOCKET _sock, AutoBuffer &_buffer, size_t _max_size, int &_errcode, int _timeout,
	     bool _wait_full_size) override;

	virtual std::string ErrorDesc(int _errcode) override;
    
    std::string Identify(SOCKET _sock) const override;
    int Protocol() const override{
        return Task::kTransportProtocolTCP;
    }
    virtual SOCKET CreateStream(SOCKET _sock) override{
        return INVALID_SOCKET;
    }
private:
    std::shared_ptr<comm::MComplexConnect> observer_;
	comm::SocketBreaker sBreaker_;
};

}
}


#endif //MMNET_TCPSOCKETOPERATOR_H
