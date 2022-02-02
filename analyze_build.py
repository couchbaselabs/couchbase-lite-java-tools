"""
Paw through test results to see what happened
"""
import os
import shutil
import sys
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

WORKING_DIR = ".build_analysis"
LATESTBUILDS = "http://latestbuilds.service.couchbase.com/builds/latestbuilds/"
JAVA_VARIANTS = ["linux", "macos", "windows"]


def delete_quietly(path):
    try:
        shutil.rmtree(path)
    except OSError:
        pass


def get_reports(path):
    url = LATESTBUILDS + path
    try:
        urllib.request.urlretrieve(url, "report.zip")
        if not os.path.isfile("report.zip"):
            raise Exception("No reports at URL: " + url)
        with zipfile.ZipFile("report.zip", 'r') as zip:
            zip.extractall(".")
    finally:
        delete_quietly("report.zip")


def analyze_tests(platform, root):
    print("Checking platform: {platform}".format(platform=platform))
    reports = [name for name in os.listdir(root) if name.endswith(".xml") and os.path.isfile(os.path.join(root, name))]
    if len(reports) <= 1:
        raise Exception("No reports for platform: " + platform)
    for file in reports:
        for failure in ET.parse(os.path.join(root, file)).getroot().findall("./testcase[failure]"):
            failed_test = failure.attrib
            print("FAILED TEST: {klass}.{test}".format(klass=failed_test["classname"], test=failed_test["name"]))


def check_java(build_num):
    for platform in JAVA_VARIANTS:
        try:
            get_reports(
                "couchbase-lite-java/3.1.0/{n}/test-reports-{platform}-ee.zip".format(n=build_num, platform=platform))
            analyze_tests(platform, "test/raw")
        except Exception as e:
            print(str(e))
        finally:
            delete_quietly("test")


# should print the device name, somehow?
def check_android(build_num):
    try:
        get_reports("couchbase-lite-android/3.1.0/{n}/test-reports-android-ee.zip".format(n=build_num))
        analyze_tests("android", "connected/raw")
    except Exception as e:
        print(str(e))
    finally:
        delete_quietly("connected")


def main():
    if len(sys.argv) != 3:
        sys.exit("Usage: python analyze_build.py <android build #> <java build #>")

    delete_quietly(WORKING_DIR)
    try:
        os.mkdir(WORKING_DIR)

        try:
            os.chdir(WORKING_DIR)
            check_android(sys.argv[1])
            check_java(sys.argv[2])
        finally:
            os.chdir("../")

    finally:
        delete_quietly(WORKING_DIR)


if __name__ == "__main__":
    main()
