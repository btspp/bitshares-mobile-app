package com.btsplusplus.fowallet.gateway

import android.content.Context
import bitshares.*
import com.btsplusplus.fowallet.R
import org.json.JSONArray
import org.json.JSONObject

class XBTS : GatewayBase() {

    override fun queryCoinList(): Promise {
        val api_base = _api_config_json.getString("base")
        val coin_list = _api_config_json.getString("coin_list")

        return OrgUtils.asyncJsonGet("$api_base$coin_list")
    }

    override fun processCoinListData(data_array: JSONArray, balanceHash: JSONObject): JSONArray? {
        val result = JSONArray()
        for (it in data_array.forin<JSONObject>()) {
            val item = it!!
            val enableDeposit = item.isTrue("depositAllowed")
            val enableWithdraw = item.isTrue("withdrawalAllowed")
            val n_minAmount = bigDecimalfromAmount(item.getString("minAmount"), item.getInt("precision"))

            //  网络确认数
            var confirm_block_number = ""
            val confirmations = item.optJSONObject("confirmations")
            if (confirmations != null && confirmations.getString("type").equals("blocks", true)) {
                confirm_block_number = confirmations.getString("value")
            }

            val symbol = item.getString("symbol").toUpperCase()
            val balance_item = balanceHash.optJSONObject(symbol)
                    ?: jsonObjectfromKVS("iszero", true)

            val appext = GatewayAssetItemData()
            appext.enableWithdraw = enableWithdraw
            appext.enableDeposit = enableDeposit
            appext.symbol = symbol
            appext.backSymbol = item.getString("backingCoin").toUpperCase()
            appext.name = item.getString("name")
            appext.intermediateAccount = item.optString("issuerId") ?: item.optString("issuer")
            appext.balance = balance_item
            appext.depositMinAmount = n_minAmount.toString()
            appext.withdrawMinAmount = n_minAmount.toString()
            appext.withdrawGateFee = ""
            appext.supportMemo = item.getBoolean("memoSupport")
            appext.confirm_block_number = confirm_block_number
            appext.coinType = item.getString("symbol")
            appext.backingCoinType = item.getString("backingCoin")

            item.put("kAppExt", appext)
            result.put(item)
        }
        return result
    }

    /**
     *  请求充值地址
     */
    override fun requestDepositAddress(item: JSONObject, fullAccountData: JSONObject, ctx: Context): Promise {
        val appext = item.get("kAppExt") as GatewayAssetItemData
        val account_data = fullAccountData.getJSONObject("account")

        //  if memo not supported - should request deposit address
        if (!item.isTrue("memoSupport")) {
            val walletType = item.getString("walletType")
            val request_deposit_address_base = _api_config_json.getString("request_deposit_address")
            val final_url = "${_api_config_json.getString("base")}${String.format(request_deposit_address_base, walletType)}"
            return requestDepositAddressCore(item, appext, final_url, fullAccountData, ctx)
        }

        //  if support memo (memo is fixed now = dex:bitshares-account-name eg. dex:btsacc)
        var inputAddress: String? = null
        val gatewayWallet = item.optString("gatewayWallet")
        if (gatewayWallet != "") {
            inputAddress = gatewayWallet
        }
        val account_name = account_data.getString("name")
        val inputMemo = "dex:$account_name"
        val p = Promise()
        if (inputAddress != null) {
            val depositItem = JSONObject().apply {
                put("inputAddress", inputAddress)
                put("inputCoinType", appext.backingCoinType.toLowerCase())
                put("inputMemo", inputMemo)
                put("outputAddress", account_name)
                put("outputCoinType", appext.coinType.toLowerCase())
            }
            p.resolve(depositItem)
        } else {
            p.resolve(R.string.kVcDWErrTipsRequestDepositAddrFailed.xmlstring(ctx))
        }
        return p
    }

    /**
     *  验证地址是否有效
     */
    override fun checkAddress(item: JSONObject, address: String, memo: String?, amount: String): Promise {

        //  TODO:仅验证地址
        val walletType = item.getString("walletType")

        val check_address_base = _api_config_json.getString("check_address")
        val api_base = _api_config_json.getString("base")
        val final_url = "$api_base${String.format(check_address_base, walletType)}"

        val p = Promise()
        OrgUtils.asyncPost_jsonBody(final_url, jsonObjectfromKVS("address", address)).then {
            val json = it as? JSONObject
            if (json != null && json.isTrue("isValid")) {
                p.resolve(true)
            } else {
                p.resolve(false)
            }
            return@then null
        }.catch {
            p.resolve(false)
        }
        return p
    }

    /**
     *  (public) 查询提币网关中间账号以及转账需要备注的memo信息。
     */
    override fun queryWithdrawIntermediateAccountAndFinalMemo(appext: GatewayAssetItemData, address: String, memo: String?, intermediateAccountData: JSONObject?): Promise {
        //  XBTS 格式
        assert(intermediateAccountData != null)
        //  TODO:fowallet 很多特殊处理
        //  useFullAssetName        - 部分网关提币备注资产名需要 网关.资产
        //  assetWithdrawlAlias     - 部分网关部分币种提币备注和bts上资产名字不同。
        //  REMARK：XBTS 网关背书资产需要小写。
        val assetName = appext.backSymbol.toLowerCase()
        val final_memo = if (memo != null && memo != "") {
            String.format("%s:%s:%s", assetName, address, memo)
        } else {
            String.format("%s:%s", assetName, address)
        }
        return Promise._resolve(JSONObject().apply {
            put("intermediateAccount", appext.intermediateAccount)
            put("finalMemo", final_memo)
            put("intermediateAccountData", intermediateAccountData)
        })
    }
}