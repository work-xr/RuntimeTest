#
# Copyright (C) 2007 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

TARGET_PLATFORM := sc9820
TARGET_HARDWARE := sc9820a
TARGET_BOARD := sp9820a_c110

PLATDIR := device/sprd/scx35l
PLATCOMM := $(PLATDIR)/common
BOARDDIR := $(PLATDIR)/$(TARGET_BOARD)
ROOTDIR := $(BOARDDIR)/rootdir
ROOTCOMM := $(PLATCOMM)/rootdir

# include vendor/sprd/partner/prebuilt_apps/Nikie.mk

include $(APPLY_PRODUCT_REVISION)
BOARD_KERNEL_PAGESIZE := 2048
BOARD_KERNEL_SEPARATED_DT := true

STORAGE_INTERNAL := emulated
STORAGE_PRIMARY := external

VOLTE_SERVICE_ENABLE := true

#use sprd's four(wifi bt gps fm) integrated one chip
USE_SPRD_WCN := true

# copy media_codec.xml before calling device.mk,
# because we want to use our file, not the common one
PRODUCT_COPY_FILES += $(BOARDDIR)/media_codecs.xml:system/etc/media_codecs.xml

# SPRD:resolve the primary card can't be recorgnized {@
ifndef STORAGE_ORIGINAL
  STORAGE_ORIGINAL := false
endif

ifndef ENABLE_OTG_USBDISK
  ENABLE_OTG_USBDISK := false
endif
# @}
# include general common configs
$(call inherit-product, $(PLATCOMM)/device.mk)
$(call inherit-product, $(PLATCOMM)/nand/nand_device.mk)
$(call inherit-product, $(PLATCOMM)/proprietories.mk)

#SPRD: add for low ram configs.must after calling device.mk
$(call inherit-product, $(PLATCOMM)/feature_phone_256_ram_conf.mk)

DEVICE_PACKAGE_OVERLAYS := $(BOARDDIR)/overlay $(PLATCOMM)/overlay
# Remove video wallpaper feature
PRODUCT_VIDEO_WALLPAPERS := none
BUILD_FPGA := false
PRODUCT_AAPT_CONFIG := ldpi mdpi small dpad
PRODUCT_AAPT_PREF_CONFIG := ldpi

# Set default USB interface
PRODUCT_DEFAULT_PROPERTY_OVERRIDES += \
	persist.sys.usb.config=mtp

PRODUCT_PROPERTY_OVERRIDES += \
	keyguard.no_require_sim=true \
	ro.com.android.dataroaming=false \
	ro.msms.phone_count=2 \
        ro.modem.l.count=2 \
	persist.msms.phone_count=2 \
        persist.radio.multisim.config=dsds \
	persist.msms.phone_default=0 \
        persist.sys.modem.diag=,gser \
        sys.usb.gser.count=8 \
        ro.modem.external.enable=0 \
        persist.sys.support.vt=false \
        persist.modem.l.cs=0 \
        persist.modem.l.ps=1 \
        persist.modem.l.rsim=1 \
        persist.radio.ssda.mode=tdd-csfb \
        persist.radio.ssda.testmode=8 \
        persist.radio.ssda.testmode1=10 \
        persist.radio.cmcc.priority=true \
        persist.support.oplpnn=true \
        persist.support.cphsfirst=false \
        lmk.autocalc=false \
        use_brcm_fm_chip=true \
        ro.wcn.gpschip=ge2 \
        ro.digital.fm.support=0
ifeq ($(strip $(VOLTE_SERVICE_ENABLE)), true)
PRODUCT_PROPERTY_OVERRIDES += persist.sys.volte.enable=true
endif

# board-specific modules
PRODUCT_PACKAGES += \
        sensors.sc8830 \
        hwcomposer.$(TARGET_BOARD_PLATFORM) \
        sprd_gsp.$(TARGET_BOARD_PLATFORM) \
        fm.$(TARGET_PLATFORM) \
        ValidationTools \
	download \
	gnss_download \
        ims 


#[[ for autotest
#        PRODUCT_PACKAGES += autotest
#]]

PRODUCT_PACKAGES += wpa_supplicant \
	wpa_supplicant.conf \
	wpa_supplicant_overlay.conf \
	hostapd

PRODUCT_PACKAGES += \
        lss_service-debug

# Sprd TTS Engine
PRODUCT_PACKAGES += SprdTts

PRODUCT_PACKAGES += \
        SimpleHome \
        RuntimeTest

#PRODUCT_PACKAGES += \
#        SOS \
#        CallFireWall

#Sprd DM
#PRODUCT_PACKAGES += \
#    OpManager \
#    Provision2

# board-specific files
PRODUCT_COPY_FILES += \
	$(BOARDDIR)/slog_modem_$(TARGET_BUILD_VARIANT).conf:system/etc/slog_modem.conf \
	$(BOARDDIR)/test_mode_$(TARGET_BUILD_VARIANT).conf:system/etc/test_mode.conf \
	$(ROOTDIR)/prodnv/PCBA.conf:prodnv/PCBA.conf \
	$(ROOTDIR)/prodnv/BBAT.conf:prodnv/BBAT.conf \
	$(ROOTDIR)/system/usr/keylayout/sprd-gpio-keys.kl:system/usr/keylayout/sprd-gpio-keys.kl \
	$(ROOTDIR)/system/usr/keylayout/sprd-eic-keys.kl:system/usr/keylayout/sprd-eic-keys.kl \
	$(ROOTDIR)/system/usr/keylayout/sci-keypad-ext.kl:system/usr/keylayout/sci-keypad-ext.kl \
	$(ROOTDIR)/root/init.$(TARGET_BOARD).2342.rc:root/init.$(TARGET_BOARD).rc \
	$(ROOTDIR)/root/init.recovery.$(TARGET_BOARD).2342.rc:root/init.recovery.$(TARGET_BOARD).rc \
	$(ROOTDIR)/system/etc/audio_params/tiny_hw.xml:system/etc/tiny_hw.xml \
	$(ROOTDIR)/system/etc/audio_params/codec_pga.xml:system/etc/codec_pga.xml \
	$(ROOTDIR)/system/etc/audio_params/audio_hw.xml:system/etc/audio_hw.xml \
	$(ROOTDIR)/system/etc/audio_params/audio_para:system/etc/audio_para \
	$(ROOTDIR)/system/etc/audio_params/audio_policy.conf:system/etc/audio_policy.conf \
	$(ROOTCOMM)/root/ueventd.sc8830.rc:root/ueventd.$(TARGET_BOARD).rc \
	$(ROOTCOMM)/system/usr/idc/focaltech_ts.idc:system/usr/idc/focaltech_ts.idc \
	$(ROOTCOMM)/system/usr/idc/msg2138_ts.idc:system/usr/idc/msg2138_ts.idc \
	$(ROOTCOMM)/system/usr/idc/goodix_ts.idc:system/usr/idc/goodix_ts.idc \
	$(ROOTCOMM)/system/usr/idc/SITRONIX.idc:system/usr/idc/SITRONIX.idc \
	$(ROOTCOMM)/system/usr/idc/ITE7258.idc:system/usr/idc/ITE7258.idc \
	frameworks/native/data/etc/android.hardware.camera.front.xml:system/etc/permissions/android.hardware.camera.front.xml \
        frameworks/native/data/etc/android.hardware.location.gps.xml:system/etc/permissions/android.hardware.location.gps.xml \
	frameworks/native/data/etc/android.hardware.usb.host.xml:system/etc/permissions/android.hardware.usb.host.xml \
	frameworks/native/data/etc/android.hardware.sensor.light.xml:system/etc/permissions/android.hardware.sensor.light.xml \
	frameworks/native/data/etc/android.hardware.sensor.proximity.xml:system/etc/permissions/android.hardware.sensor.proximity.xml \
	frameworks/native/data/etc/android.hardware.sensor.accelerometer.xml:system/etc/permissions/android.hardware.sensor.accelerometer.xml\
	frameworks/native/data/etc/android.hardware.camera.autofocus.xml:system/etc/permissions/android.hardware.camera.autofocus.xml\
	frameworks/native/data/etc/android.hardware.wifi.xml:system/etc/permissions/android.hardware.wifi.xml
#	hardware/broadcom/libbt/conf/bcm/firmware/bcm4343s/bcm4343.hcd:system/vendor/firmware/bcm4343.hcd

$(call inherit-product-if-exists, vendor/sprd/open-source/common_packages.mk)
$(call inherit-product-if-exists, vendor/sprd/open-source/plus_special_packages.mk)
$(call inherit-product, vendor/sprd/partner/shark/bluetooth/device-shark-bt.mk)
$(call inherit-product, vendor/sprd/gps/GreenEye2/device-sprd-gps.mk)
$(call inherit-product, vendor/sprd/partner/prebuilt_apps/thirdparty_apk.mk)
$(call inherit-product, vendor/sprd/partner/prebuilt_apps/copy_lib.mk)
$(call inherit-product-if-exists, vendor/sprd/open-source/res/boot/boot_res_watch.mk)

# select WCN
BOARD_HAVE_BLUETOOTH := true
BOARD_SPRD_WCNBT_MARLIN := true
WCN_EXTENSION := true
ifeq ($(strip $(USE_SPRD_WCN)),true)
#connectivity configuration
CONNECTIVITY_HW_CONFIG := $(TARGET_BOARD)
#CONNECTIVITY_HW_CHISET := $(shell grep BOARD_SPRD_WCNBT $(BOARDDIR)/BoardConfig.mk)
BOARD_HAVE_SPRD_WCN_COMBO := marlin
$(call inherit-product, vendor/sprd/open-source/res/connectivity/device-sprd-wcn.mk)
endif

# Overlay touch screen features, and all core feature should edit here, not modify directly
PRODUCT_COPY_FILES := \
        $(BOARDDIR)/handheld_core_hardware.xml:system/etc/permissions/handheld_core_hardware.xml \
        $(PRODUCT_COPY_FILES)

# add security build info
# $(call inherit-product, vendor/sprd/open-source/security_support.mk)


# PRODUCT_REVISION := multiuser
# include $(APPLY_PRODUCT_REVISION)

CHIPRAM_DEFCONFIG := sp9820a_c110
KERNEL_DEFCONFIG := sp9820a_c110_defconfig
DTS_DEFCONFIG := sprd-scx35l_sp9820a_c110_tds
UBOOT_DEFCONFIG := sp9820a_c110
UBOOT_CONFIG_PRODUCT := sp9820a_c110_tds
KERNEL_CONFIG_PRODUCT:= sp9820a_c110_tds
# Overrides
PRODUCT_NAME := sp9820a_c110_tds
PRODUCT_DEVICE := $(TARGET_BOARD)
PRODUCT_MODEL := E5
PRODUCT_BRAND := SPRD
PRODUCT_MANUFACTURER := SPRD


    PRODUCT_LOCALES := zh_CN zh_TW en_US


#config selinux policy
BOARD_SEPOLICY_DIRS += $(PLATCOMM)/sepolicy

#modify browser for 9820
PRODUCT_PROPERTY_OVERRIDES += \
    ro.browser.bottombar=false \
    ro.browser.navigationtab=false \
    ro.product.barphone=true \
    ro.browser.carrier.homepage=false

include $(wildcard vendor/sprd/proprietories-source/prebuilts/BaiduNetworkLocation/setup.mk)
include $(wildcard vendor/sprd/proprietories/prebuilts/BaiduNetworkLocation/setup.mk)

# resource for SprdTts
ifneq (,$(wildcard vendor/sprd/platform/packages/apps/SprdTts/assets/Resource.irf))
PRODUCT_COPY_FILES += \
    vendor/sprd/platform/packages/apps/SprdTts/assets/Resource.irf:system/tts/Resource.irf
endif

PRODUCT_PROPERTY_OVERRIDES += \
    persist.sys.ucam.all=false \
    persist.sys.ucam.beauty=false \
    persist.sys.ucam.filter=false \
    persist.sys.ucam.edit=false \
    persist.sys.ucam.puzzle=false \
    persist.sys.cam.vgesture=false \
    persist.sys.cam.gif=false \
    persist.sys.cam.timestamp=false \
    persist.sys.cam.scenery=false \
    persist.sys.cam.wideangle=false \
    persist.sys.cam.slow_motion=false \
    persist.sys.cam.quick=false \
    persist.sys.cam.eois.dc.back=false \
    persist.sys.cam.eois.dc.front=false \
    persist.sys.cam.eois.dv.back=false \
    persist.sys.cam.eois.dv.front=false \
    persist.sys.cam.ninetynine=false \
    persist.sys.cam.burst=false \
    persist.sys.cam.zsl=false \
    persist.sys.cam.highiso=false \
    persist.sys.cam.voicephoto=false \
    persist.front.camera.mirror=false \
    persist.sys.cam.gps=false \
    persist.sys.cam.qrcode=false \
    ro.operator.smartsms=true \
    dalvik.vm.jit.codecachesize=0

#config inputmethod app
PRODUCT_PROPERTY_OVERRIDES += \
    ro.product.inputmethod = iFly

#config simplehome for 9820
PRODUCT_PROPERTY_OVERRIDES += \
    ro.home.flashlight.centerkey=true
BOARD_HAVE_MARLIN := true
BOARD_HAVE_FM_TROUT := true
BOARD_USE_SPRD_FMAPP := true
SPRD_EXTERNAL_WCN :=true
BOARD_HAVE_BLUETOOTH_SPRD := true
BOARD_HAVE_NANDFLASH := true
BOARD_USE_SPRD_GNSS := ge2
