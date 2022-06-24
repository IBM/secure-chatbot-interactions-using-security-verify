/**
  *
  * main() will be run when you invoke this action
  *
  * @param Cloud Functions actions accept a single parameter, which must be a JSON object.
  *
  * @return The output of this action, which must be a JSON object.
  *
  */
 
//Pre-requisites
var axios = require('axios');
var qs = require('qs');

//Security Verify Details
var tenant_url = "shikham.verify.ibm.com";
var client_id = 'b06dad03-5204-4df8-b59f-deac810c8b82';
var client_secret = "WwY7avA2Xt";

//API Details
var GET_ACTIVE_ORDERS_URL='http://ecomm-portal-chatbot.cp-india-q3-f2c6cdc6801be85fd188b09d006f13e3-0000.jp-tok.containers.appdomain.cloud/portal/ecomm/auth/getactiveorders';
var GET_ALL_ORDERS_URL = 'http://ecomm-portal-chatbot.cp-india-q3-f2c6cdc6801be85fd188b09d006f13e3-0000.jp-tok.containers.appdomain.cloud/portal/ecomm/auth/getorders';
var CANCEL_ORDER_URL = 'http://ecomm-portal-chatbot.cp-india-q3-f2c6cdc6801be85fd188b09d006f13e3-0000.jp-tok.containers.appdomain.cloud/portal/ecomm/auth/cancelorder';


async function main(params) {
    let request_type = params.request_type
    console.log("Request: " + request_type);


    if ( request_type === "verify_email") {
        console.log("verify_email");
        let emailID = params.emailID;
        
        var response = await get_access_token();
        console.log(response);
        let access_token = response.access_token;
        
        var response1 = await validate_email(access_token, emailID);
        console.log(response1.totalResults);

        if ( response1.totalResults > 0 ) {
          var otp_response = await email_otp(access_token, emailID);
          console.log(otp_response);
          return({"result":1, "message":"registered emailID", "access_token":access_token, "otp_txn_id":otp_response.id});
        } else {
          console.log ("Please provide a registered emailID");
          return({"result":0, "message":"Please provide a registered emailID"});
        }
    } else if ( request_type === "verify_otp") {
        console.log("verify_otp");
        let emailID = params.emailID;
        let access_token = params.access_token;
        let otp_txn_id = params.otp_txn_id;
        let otp = params.otp;
        
        if (!access_token){
            return({"verified_user":0, "message":"Authorization failed."})
        }
        var response = await verify_otp(access_token, emailID, otp_txn_id, otp);
        console.log(response);
        
        if ( response.message ) {
            return({"verify_status":0, "message":response.message});
        } else {
          return({"verify_status":1, "message":"Great! The code verification is successful."});
        }
    } else if ( request_type === "list_active_orders") {
        let access_token = params.access_token;
        let emailID = params.emailID;
        if (!access_token){
            return({"verified_user":0, "message":"Authorization failed."})
        }
        var response = await list_active_orders(access_token, emailID);
        console.log(response);
        let arr = [];
        arr = response.Orders;
        if (arr.length != 0) {
            var output = await format_response(response.Orders);
            return(output);
        } else {
            return ({"Orders": "No active orders"});
        }
    } else if ( request_type === "list_all_orders") {
        let access_token = params.access_token;
        let emailID = params.emailID;
        if (!access_token){
            return({"verified_user":0, "message":"Authorization failed."})
        }
        var response = await list_all_orders(access_token, emailID);
        console.log(response);
        let arr = [];
        arr = response.Orders;
        if (arr.length != 0) {
            var output = await format_response(response.Orders);
            return(output);
        } else {
            return ({"Orders": "No order history"});
        }

    } else if ( request_type === "cancel_order") {
        let orderID = params.orderID;
        let access_token = params.access_token;
        if (!access_token){
            return({"verified_user":0, "message":"Authorization failed."})
        }
        var response = await cancel_order(access_token, orderID);
        console.log(response);
        return(response);

    } else {
      console.log("Unsupported Request");
      return({"message":"Unsupported request"});
    }
  }

get_access_token = async () => {
    
  var data = {
         'grant_type': 'client_credentials',
         'client_id': client_id,
         'client_secret': client_secret 
    };
        
  var request = {
    method: 'post',
    url: 'https://' + tenant_url + '/oidc/endpoint/default/token',
    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
    data : qs.stringify(data)
  };

  return new Promise(function (resolve, reject) {
    axios(request).then((response) => {
        var tokenData = response.data;
        access_token = tokenData.access_token;
        console.log(access_token);
        resolve(response.data);
    }).catch((error) => {
        console.log(error);
        reject(error);
    });
  });
}

validate_email = async (access_token, emailID) => {

  var config = {
    method: 'get',
    url: 'https://' + tenant_url + '/v2.0/Users?filter=emails.value eq "' + emailID + '"&attributes=emails',
    headers: {
      'Authorization': 'Bearer ' + access_token
    }
  };

  return new Promise(function (resolve, reject) {
    axios(config)
    .then( (response) => {
      console.log(JSON.stringify(response.data));
      resolve(response.data);
    })
    .catch(function (error) {
      console.log(error);
      reject(error);
    });
  })
}

email_otp = async (token, emailID) => {
  var data = JSON.stringify({
    "emailAddress": emailID,
    "correlation": "1234"
  });
  
  var config = {
    method: 'post',
    url: 'https://' + tenant_url + '/v2.0/factors/emailotp/transient/verifications',
    headers: { 
      'Content-Type': 'application/json', 
      'Authorization': 'Bearer ' + token,
    },
    data : data
  };
  
  return new Promise(function (resolve, reject) {
    axios(config)
    .then(function (response) {
      console.log(JSON.stringify(response.data));
      resolve(response.data);
    })
    .catch(function (error) {
      console.log(error);
      reject(error);
    });  
  })
}

verify_otp = async (token, emailID, txnID, otp) => {
  var data = JSON.stringify({
    "otp": otp
  });
  
  var config = {
    method: 'post',
    url: 'https://' + tenant_url + '/v2.0/factors/emailotp/transient/verifications/' + txnID,
    headers: { 
      'Content-Type': 'application/json', 
      'Authorization': 'Bearer ' + token
    },
    data : data
  };
  
  return new Promise(function (resolve, reject) {
      axios(config)
      .then(function (response) {
        console.log(JSON.stringify(response.data));
        resolve(response.data);
      })
      .catch(function (error) {
        console.log(error);
        reject(error);
      });  
  })
}

list_active_orders = async (access_token, emailID) => {
    console.log('Running list_active_orders API');
    var config = {
        method: 'get',
        url: GET_ACTIVE_ORDERS_URL + '?emailid=' + emailID,
        headers: { 
          'Content-Type': 'application/json',
          'Accept': 'application/json',
          'verify-token': access_token
        }
    };
    
    return new Promise(function (resolve, reject) {
      axios(config)
      .then(function (response) {
        console.log(response.data);
        resolve(response.data);
      })
      .catch(function (error) {
        console.log(error);
        reject(error);
      });  
    })
}

list_all_orders = async (access_token, emailID) => {
    console.log('Running list_all_orders API');
    var config = {
        method: 'get',
        url: GET_ALL_ORDERS_URL + '?emailid=' + emailID,
        headers: { 
          'Content-Type': 'application/json', 
          'Accept': 'application/json',
          'verify-token': access_token
        }
    };
    
    return new Promise(function (resolve, reject) {
      axios(config)
      .then(function (response) {
        console.log(response.data);
        resolve(response.data);
      })
      .catch(function (error) {
        console.log(error);
        reject(error);
      });  
    })
}

cancel_order = async (access_token, orderID) => {
    console.log('Running cancel_order API');
    var config = {
        method: 'get',
        url: CANCEL_ORDER_URL + '?orderid=' + orderID,
        headers: { 
          'Content-Type': 'application/json', 
          'verify-token':  access_token
        }
    };
    
    return new Promise(function (resolve, reject) {
      axios(config)
      .then(function (response) {
        console.log(response.data);
        resolve(response.data);
      })
      .catch(function (error) {
        console.log(error);
        reject(error);
      });  
    })
}

format_response = async (orders_arr) => {
        console.log("Total number of orders:");
        console.log(orders_arr.length);
        let items_arr=[];
        for (let i=0; i<orders_arr.length; i++){
            let object = {
                "Order ID": orders_arr[i].orderId,
                "Item": orders_arr[i].item,
                "Ordered On": orders_arr[i].orderDt,
                "Status": orders_arr[i].status
            }
            items_arr.push(object);
        }
        return ({"Orders": items_arr});
}
