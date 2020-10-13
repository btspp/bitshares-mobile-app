package com.btsplusplus.fowallet

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import bitshares.*
import com.btsplusplus.fowallet.gateway.GatewayAssetItemData
import com.btsplusplus.fowallet.gateway.GatewayBase
import com.btsplusplus.fowallet.gateway.RuDEX
import com.btsplusplus.fowallet.gateway.XBTS
import com.btsplusplus.fowallet.utils.VcUtils
import com.fowallet.walletcore.bts.ChainObjectManager
import com.fowallet.walletcore.bts.WalletManager
import kotlinx.android.synthetic.main.activity_deposit_and_withdraw.*
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigInteger

class ActivityDepositAndWithdraw : BtsppActivity() {

    private lateinit var _gatewayArray: JSONArray
    private lateinit var _currGateway: JSONObject
    private lateinit var _fullAccountData: JSONObject
    private var _data_array = mutableListOf<JSONObject>()
    private var _balanceDataHash = JSONObject()
    private var _balanceDataNameHash = JSONObject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deposit_and_withdraw)

        // 设置全屏(隐藏状态栏和虚拟导航栏)
        setFullScreen()

        //  返回按钮
        layout_back_from_page_of_recharge_and_withdraw.setOnClickListener { finish() }

        //  当前账号信息
        assert(WalletManager.sharedWalletManager().isWalletExist())
        _fullAccountData = WalletManager.sharedWalletManager().getWalletAccountInfo()!!

        //  TODO:1.6 动态加载配置数据
        val ctx = this
        _gatewayArray = JSONArray().apply {
            //  TODO:2.5 open的新api还存在部分bug，open那边再进行修复，待修复完毕之后再开放该功能。
//            // OpenLedger   API reference: https://github.com/bitshares/bitshares-ui/files/3068123/OL-gateways-api.pdf
//            put(JSONObject().apply {
//                put("name", "OpenLedger")
//                put("api", OpenLedger().initWithApiConfig(JSONObject().apply {
//                    put("base", "https://gateway.openledger.io")
//                    put("assets", "/assets")
//                    put("exchanges", "/exchanges")
//                    put("request_deposit_address", "/exchanges/%s/transfer/source/prototype")
//                    put("validate", "/exchanges/%s/transfer/destination")
//                }))
//                put("helps", JSONArray().apply {
//                    put(JSONObject().apply {
//                        put("title", R.string.kVcDWHelpTitleSupport.xmlstring(ctx))
//                        put("value", "https://openledger.freshdesk.com")
//                        put("url", true)
//                    })
//                })
//            })
            //  GDEX
            put(JSONObject().apply {
                put("name", "GDEX")
                put("api", GatewayBase().initWithApiConfig(JSONObject().apply {
                    put("base", "https://api.gdex.io/adjust")
                    put("coin_list", "/coins")
                    put("active_wallets", "/active-wallets")
                    put("trading_pairs", "/trading-pairs")
                    put("request_deposit_address", "/simple-api/initiate-trade")
                    put("check_address", "/wallets/%s/address-validator")
                }))
                put("helps", JSONArray().apply {
                    put(JSONObject().apply {
                        put("title", R.string.kVcDWHelpTitleSupport.xmlstring(ctx))
                        put("value", "https://support.gdex.io/")
                    })
                    put(JSONObject().apply {
                        put("title", R.string.kVcDWHelpTitleQQ.xmlstring(ctx))
                        put("value", "602573197")
                    })
                    put(JSONObject().apply {
                        put("title", R.string.kVcDWHelpTitleTelegram.xmlstring(ctx))
                        put("value", "https://t.me/GDEXer")
                    })
                })
            })
            //  RuDEX   API reference: https://docs.google.com/document/d/196hdHb1BTGdmuVi_w74y7lt4Acl0mqt8P02Xg4GSkcI/edit
            put(JSONObject().apply {
                put("name", "RuDEX")
                put("api", RuDEX().initWithApiConfig(JSONObject().apply {
                    put("base", "https://gateway.rudex.org/api/v0_3")
                    put("coin_list", "/coins")
                    put("request_deposit_address", "/wallets/%s/new-deposit-address")
                    put("check_address", "/wallets/%s/check-address")
                }))
                put("helps", JSONArray().apply {
                    put(JSONObject().apply {
                        put("title", R.string.kVcDWHelpTitleSupport.xmlstring(ctx))
                        put("value", "https://rudex.freshdesk.com")
                    })
                    put(JSONObject().apply {
                        put("title", "Twitter")
                        put("value", "https://twitter.com/rudex_bitshares")
                    })
                    put(JSONObject().apply {
                        put("title", R.string.kVcDWHelpTitleTelegram.xmlstring(ctx))
                        put("value", "https://t.me/BitSharesDEX_RU")
                    })
                })
            })
            put(JSONObject().apply {
                put("name", "XBTS")
                put("api", XBTS().initWithApiConfig(JSONObject().apply {
                    put("base", "https://apis.xbts.io/api/v1")
                    put("coin_list", "/coins")
                    put("request_deposit_address", "/wallets/%s/new-deposit-address")
                    put("check_address", "/wallets/%s/check-address")
                }))
                put("helps", JSONArray().apply {
                    put(JSONObject().apply {
                        put("title", "Twitter")
                        put("value", "https://twitter.com/XBTS_EXCHANGE")
                    })
                    put(JSONObject().apply {
                        put("title", R.string.kVcDWHelpTitleTelegram.xmlstring(ctx))
                        put("value", "https://t.me/xbtsio")
                    })
                })
            })

        }

        //  初始化默认网关
        assert(_gatewayArray.length() > 0)
        _currGateway = _gatewayArray.first<JSONObject>()!!
        val defaultGatewayName = R.string.appDepositWithdrawDefaultGateway.xmlstring(this)
        for (gateway in _gatewayArray.forin<JSONObject>()) {
            if (gateway!!.getString("name") == defaultGatewayName) {
                _currGateway = gateway!!
                break
            }
        }

        //  gateway assets faq button
        tip_link_gateway_assets.setOnClickListener { onGatewayAssetsFAQClicked() }

        //  current gateway
        layout_current_gateway.setOnClickListener { onCurrentGatewayClicked() }

        //  初始化UI
        gateway_assets_list_count_of_recharge_and_withdraw.text = String.format(resources.getString(R.string.kVcDWHelpGatewayAssets, "0"))
        refreshGatewayInfoUI()

        //  请求
        queryFullAccountDataAndCoinList()
    }

    private fun onGatewayAssetsFAQClicked() {
        VcUtils.gotoQaView(this, "qa_deposit_withdraw", resources.getString(R.string.kVcTitleWhatIsGatewayAssets))
    }

    private fun onCurrentGatewayClicked() {
        val list = JSONArray()
        _gatewayArray.forEach<JSONObject> { list.put(it!!.getString("name")) }
        ViewSelector.show(this, resources.getString(R.string.kVcDWTipsSelectGateway), list.toList<String>().toTypedArray()) { index: Int, _: String ->
            val selectItem = _gatewayArray.getJSONObject(index)
            if (selectItem.getString("name") != _currGateway.getString("name")) {
                _currGateway = selectItem
                refreshGatewayInfoUI()
                queryFullAccountDataAndCoinList()
            }
        }
    }

    private fun queryFullAccountDataAndCoinList() {
        val account_data = _fullAccountData.getJSONObject("account")

        val mask = ViewMask(R.string.kTipsBeRequesting.xmlstring(this), this)
        mask.show()

        val p1 = ChainObjectManager.sharedChainObjectManager().queryFullAccountInfo(account_data.getString("id"))
        val p2 = (_currGateway.get("api") as GatewayBase).queryCoinList()

        Promise.all(p1, p2).then { it ->
            val data_array = it as JSONArray
            val asset_ids = _extractAllAssetIdsFromFullAccountData(data_array[0] as? JSONObject)
            return@then ChainObjectManager.sharedChainObjectManager().queryAllAssetsInfo(asset_ids).then {
                mask.dismiss()
                onQueryResponsed(data_array)
                return@then null
            }
        }.catch {
            mask.dismiss()
            showToast(resources.getString(R.string.tip_network_error))
        }
    }

    private fun onQueryResponsed(data_array: JSONArray) {
        assert(data_array.length() == 2)

        _fullAccountData = data_array[0] as JSONObject

        //  refresh balance & on-order values
        _onCalcBalanceInfo()

        _data_array.clear()

        var data_coin_list = data_array[1] as? JSONArray
        if (data_coin_list != null) {
            (_currGateway.get("api") as GatewayBase).processCoinListData(data_coin_list, _balanceDataNameHash)?.forEach<JSONObject> {
                _data_array.add(it!!)
            }
        }

        //  refresh ui
        refreshGatewayAssetsUI()
    }

    /**
     *  计算网关资产可用余额和冻结余额信息。
     */
    private fun _onCalcBalanceInfo() {
        _balanceDataHash = JSONObject()
        _balanceDataNameHash = JSONObject()

        //  计算所有资产的总挂单量信息
        val limit_orders_values = JSONObject()
        val limit_orders = _fullAccountData.optJSONArray("limit_orders")
        if (limit_orders != null) {
            for (order in limit_orders) {
                //  限价单卖 base 资产，卖的数量为 for_sale 字段。sell_price 只是价格信息。
                val sell_asset_id = order!!.getJSONObject("sell_price").getJSONObject("base").getString("asset_id")
                val sell_amount = BigInteger(order.getString("for_sale"))
                //  所有挂单累加
                var value = limit_orders_values.opt(sell_asset_id) as? BigInteger
                value = value?.add(sell_amount) ?: sell_amount
                limit_orders_values.put(sell_asset_id, value)
            }
        }

        //  遍历所有可用余额
        _fullAccountData.getJSONArray("balances").forEach<JSONObject> {
            val balance_item = it!!
            val balance_value = balance_item.getLong("balance")
            if (balance_value > 0) {
                val asset_type = balance_item.getString("asset_type")
                val order_value = limit_orders_values.opt(asset_type) ?: BigInteger.ZERO
                _balanceDataHash.put(asset_type, JSONObject().apply {
                    put("free", BigInteger(balance_item.getString("balance")))
                    put("order", order_value)
                    put("asset_id", asset_type)
                })
            }
        }

        //  遍历所有挂单
        for (asset_id in limit_orders_values.keys()) {
            //  已经存在添加了
            if (_balanceDataHash.has(asset_id)) {
                continue
            }
            //  添加仅挂单存在余额为0的条目。
            val order_value = limit_orders_values.get(asset_id)
            _balanceDataHash.put(asset_id, JSONObject().apply {
                put("free", BigInteger.ZERO)
                put("order", order_value)
                put("asset_id", asset_id)
            })
        }

        //  填充 _balanceDataNameHash。
        val chainMgr = ChainObjectManager.sharedChainObjectManager()
        _balanceDataHash.keys().forEach { asset_id ->
            val obj = chainMgr.getChainObjectByID(asset_id)
            val item = _balanceDataHash.getJSONObject(asset_id)
            val asset_symbol = obj.getString("symbol").toUpperCase()
            _balanceDataNameHash.put(asset_symbol, item)
        }
    }

    /**
     *  获取所有相关资产ID
     */
    private fun _extractAllAssetIdsFromFullAccountData(fullAccountData: JSONObject?): JSONArray {
        if (fullAccountData == null) {
            return JSONArray()
        }
        val result = JSONObject()
        val limit_orders = fullAccountData!!.optJSONArray("limit_orders")
        if (limit_orders != null && limit_orders.length() > 0) {
            limit_orders.forEach<JSONObject> {
                val order = it!!
                val sell_asset_id = order.getJSONObject("sell_price").getJSONObject("base").getString("asset_id")
                result.put(sell_asset_id, true)
            }
        }
        fullAccountData!!.getJSONArray("balances").forEach<JSONObject> {
            val balance_item = it!!
            val asset_type = balance_item.getString("asset_type")
            result.put(asset_type, true)
        }
        return result.keys().toJSONArray()
    }

    private fun refreshGatewayInfoUI() {
        //  gateway name
        gateway_name_of_recharge_and_withdraw.text = _currGateway.getString("name")

        //  help rows
        layout_help_of_recharge_and_withdraw.removeAllViews()
        _currGateway.getJSONArray("helps").forEach<JSONObject> {
            val layout_params = LinearLayout.LayoutParams(LLAYOUT_MATCH, LLAYOUT_WARP)
            layout_params.setMargins(0, 10.dp, 0, 0)
            val layout = LinearLayout(this)
            layout.layoutParams = layout_params
            layout.setPadding(10.dp, 0, 15.dp, 0)

            val tv_name = TextView(this)
            tv_name.text = it!!.getString("title")
            tv_name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.0f)
            tv_name.setTextColor(resources.getColor(R.color.theme01_textColorMain))

            val tv_value_params = LinearLayout.LayoutParams(LLAYOUT_MATCH, LLAYOUT_WARP)
            val tv_value = TextView(this)
            tv_value_params.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            tv_value.layoutParams = tv_value_params

            val value = it!!.getString("value")
            tv_value.text = value
            tv_value.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.0f)
            tv_value.gravity = Gravity.RIGHT or Gravity.CENTER_VERTICAL
            tv_value.setTextColor(resources.getColor(R.color.theme01_textColorMain))

            layout.apply {
                addView(tv_name)
                addView(tv_value)
            }

            //  复制
            layout.setOnClickListener {
                if (Utils.copyToClipboard(this, value)) {
                    showToast(R.string.kVcDWTipsCopyOK.xmlstring(this))
                }
            }

            layout_help_of_recharge_and_withdraw.addView(layout)
        }
    }

    private fun refreshGatewayAssetsUI() {
        //  gateway assets title view
        gateway_assets_list_count_of_recharge_and_withdraw.text = String.format(resources.getString(R.string.kVcDWHelpGatewayAssets, _data_array.size.toString()))

        //  draw assets list
        val layout_parent = layout_gateway_list_of_recharge_and_withdraw
        layout_parent.removeAllViews()
        if (_data_array.size > 0) {
            for (item in _data_array) {
                drawOneAssetCell(layout_parent, item)
            }
        } else {
            layout_parent.addView(ViewUtils.createEmptyCenterLabel(this, R.string.kVcDWTipsGatewayNotAvailable.xmlstring(this)))
        }
    }

    private fun drawOneAssetCell(layout_parent: LinearLayout, item: JSONObject) {
        val appext = item.get("kAppExt") as GatewayAssetItemData

        val name = appext.symbol
        val balance = appext.balance
        var strFreeValue = "0"
        var strOrderValue = "0"
        if (!balance.optBoolean("iszero")) {
            val asset = ChainObjectManager.sharedChainObjectManager().getChainObjectByID(balance.getString("asset_id"))
            val precision = asset.getInt("precision")
            strFreeValue = OrgUtils.formatAssetString(balance.getString("free"), precision)
            strOrderValue = OrgUtils.formatAssetString(balance.getString("order"), precision)
        }

        val layout = LinearLayout(this)
        val layout_params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layout.orientation = LinearLayout.VERTICAL
        layout.layoutParams = layout_params

        //  第一行  资产名 充币 提币
        val layout_line1 = LinearLayout(this)
        val layout_line1_params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layout_line1.orientation = LinearLayout.HORIZONTAL
        layout_line1_params.setMargins(0, 10.dp, 0, 10.dp)
        layout_line1.layoutParams = layout_line1_params

        val tv_name = TextView(this)
        tv_name.text = name
        tv_name.setTextColor(resources.getColor(R.color.theme01_textColorMain))
        tv_name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16.0f)

        //  充币
        var textColor: Int
        var backColor: Int
        if (appext.enableDeposit) {
            textColor = resources.getColor(R.color.theme01_textColorMain)
            backColor = resources.getColor(R.color.theme01_textColorHighlight)
        } else {
            textColor = resources.getColor(R.color.theme01_textColorNormal)
            backColor = resources.getColor(R.color.theme01_textColorGray)
        }
        val layout_tv_recharge = LinearLayout(this)
        val layout_tv_recharge_params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layout_tv_recharge.layoutParams = layout_tv_recharge_params
        layout_tv_recharge.gravity = Gravity.RIGHT
        val tv_recharge = TextView(this)
        tv_recharge.text = resources.getString(R.string.kVcDWCellBtnNameDeposit)
        tv_recharge.setTextColor(textColor)
        tv_recharge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.0f)
        tv_recharge.paint.isFakeBoldText = true
        tv_recharge.setBackgroundColor(backColor)
        tv_recharge.gravity = Gravity.RIGHT
        tv_recharge.setPadding(20.dp, 5.dp, 20.dp, 5.dp)

        //  提币
        if (appext.enableWithdraw) {
            textColor = resources.getColor(R.color.theme01_textColorMain)
            backColor = resources.getColor(R.color.theme01_textColorHighlight)
        } else {
            textColor = resources.getColor(R.color.theme01_textColorNormal)
            backColor = resources.getColor(R.color.theme01_textColorGray)
        }
        val tv_withdraw = TextView(this)
        val tv_withdraw_layout_params = LinearLayout.LayoutParams(LLAYOUT_WARP, LLAYOUT_WARP)
        tv_withdraw_layout_params.setMargins(10.dp, 0, 0, 0)
        tv_withdraw.layoutParams = tv_withdraw_layout_params
        tv_withdraw.text = resources.getString(R.string.kVcDWCellBtnNameWithdraw)
        tv_withdraw.setTextColor(textColor)
        tv_withdraw.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.0f)
        tv_withdraw.paint.isFakeBoldText = true
        tv_withdraw.setBackgroundColor(backColor)
        tv_withdraw.gravity = Gravity.RIGHT
        tv_withdraw.setPadding(20.dp, 5.dp, 20.dp, 5.dp)

        layout_tv_recharge.addView(tv_recharge)
        layout_tv_recharge.addView(tv_withdraw)
        layout_line1.addView(tv_name)
        layout_line1.addView(layout_tv_recharge)

        //  第二行 可用 挂单
        val layout_line2 = LinearLayout(this)
        val layout_line2_params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layout_line2.orientation = LinearLayout.HORIZONTAL
        layout_line2.layoutParams = layout_line2_params
        layout_line2_params.setMargins(0, 0, 0, 10.dp)

        val tv_available = TextView(this)
        tv_available.text = "${R.string.kLableAvailable.xmlstring(this)} ${strFreeValue}"
        tv_available.setTextColor(resources.getColor(R.color.theme01_textColorNormal))
        tv_available.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.0f)

        val layout_tv_withdraw = LinearLayout(this)
        val layout_tv_withdraw_params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        layout_tv_withdraw.layoutParams = layout_tv_withdraw_params
        layout_tv_withdraw.gravity = Gravity.RIGHT

        val tv_count = TextView(this)
        tv_count.text = "${R.string.kVcAssetOnOrder.xmlstring(this)} ${strOrderValue}"
        tv_count.setTextColor(resources.getColor(R.color.theme01_textColorNormal))
        tv_count.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14.0f)
        tv_count.setPadding(0, 0, 0.dp, 0)

        //  线
        val view_line = ViewLine(this)

        layout_tv_withdraw.addView(tv_count)
        layout_line2.addView(tv_available)
        layout_line2.addView(layout_tv_withdraw)

        layout.addView(layout_line1)
        layout.addView(layout_line2)
        layout.addView(view_line)

        layout_parent.addView(layout)

        //  冲提事件
        tv_recharge.setOnClickListener { onButtonDepositClicked(item) }
        tv_withdraw.setOnClickListener { onButtonWithdrawClicked(item) }
    }

    private fun onButtonDepositClicked(item: JSONObject) {
        val appext = item.get("kAppExt") as GatewayAssetItemData
        if (!appext.enableDeposit) {
            showToast(R.string.kVcDWTipsDisableDeposit.xmlstring(this))
            return
        }

        //  获取充币地址
        val ctx = this
        val mask = ViewMask(R.string.kTipsBeRequesting.xmlstring(ctx), ctx)
        mask.show()
        (_currGateway.get("api") as GatewayBase).requestDepositAddress(item, _fullAccountData, this).then {
            //  错误处理
            val deposit_error = it as? String
            if (deposit_error != null) {
                mask.dismiss()
                showToast(deposit_error)
                return@then null
            }
            val deposit_item = it as JSONObject
            //  create qrcode
            Utils.asyncCreateQRBitmap(deposit_item.optString("inputAddress"), 150.dp).then { btm ->
                mask.dismiss()
                //  转到充币界面
                goTo(ActivityGatewayDeposit::class.java, true, args = JSONObject().apply {
                    put("fullAccountData", _fullAccountData)
                    put("depositAddrItem", deposit_item)
                    put("depositAssetItem", item)
                    put("qrbitmap", btm!!)
                    put("title", String.format(R.string.kVcTitleDeposit.xmlstring(ctx), appext.symbol))
                })
            }
            return@then null
        }
    }

    private fun onButtonWithdrawClicked(item: JSONObject) {
        val appext = item.get("kAppExt") as GatewayAssetItemData
        if (!appext.enableWithdraw) {
            showToast(R.string.kVcDWTipsDisableWithdraw.xmlstring(this))
            return
        }

        val ctx = this
        _queryGatewayIntermediateAccountInfo(appext).then {
            val err_nil_full_data = it
            if (err_nil_full_data != null && err_nil_full_data is String) {
                showToast(err_nil_full_data)
                return@then null
            }

            //  转到提币界面
            val result_promise = Promise()
            goTo(ActivityGatewayWithdraw::class.java, true, args = JSONObject().apply {
                put("fullAccountData", _fullAccountData)
                put("intermediateAccount", err_nil_full_data)
                put("withdrawAssetItem", item)
                put("gateway", _currGateway)
                put("result_promise", result_promise)
                put("title", String.format(R.string.kVcTitleWithdraw.xmlstring(ctx), appext.symbol))
            })
            result_promise.then { dirty ->
                if (dirty as Boolean) {
                    //  提币后刷新列表
                    queryFullAccountDataAndCoinList()
                }
            }
            return@then null
        }
    }

    private fun _queryGatewayIntermediateAccountInfo(appext: GatewayAssetItemData): Promise {
        val ctx = this
        val intermediateAccount = appext.intermediateAccount
        val p = Promise()
        if (intermediateAccount != null && intermediateAccount != "") {
            val mask = ViewMask(R.string.kTipsBeRequesting.xmlstring(this), this)
            mask.show()
            ChainObjectManager.sharedChainObjectManager().queryFullAccountInfo(intermediateAccount).then {
                mask.dismiss()
                val full_data = it as? JSONObject
                if (full_data == null) {
                    p.resolve(R.string.kVcDWWithdrawQueryGatewayAccountFailed.xmlstring(ctx))
                    return@then null
                }
                p.resolve(full_data)
                return@then null
            }.catch {
                mask.dismiss()
                showToast(R.string.tip_network_error.xmlstring(ctx))
            }
        } else {
            //  null full account data
            p.resolve(null)
        }
        return p
    }
}