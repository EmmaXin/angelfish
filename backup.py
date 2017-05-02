import unittest
import json
# from datetime import datetime
import os
import errno
import shutil

class Backup:

    prefix = os.path.join(os.getcwd(), 'cfg')
    meta_file_name = 'meta.json'

    def __init__(self, name, calendar=None):
        self.calendar = calendar
        self.name = name
        self.meta = {}

    def init(self):
        self.meta = self.read_meta_file()

    def fileNameFormat(self, calendar):
        # return self.name + '-' + calendar.toString()
        pass

    def calendarToPath(self, calendar):
        # return path.join(self.root, self.name, fileNameFormat(calendar))
        fileName = 'test-2010-01-01T00-00-00.json'
        return os.path.join(self.prefix, 'test', fileName)

    def get_meta_path(self):
        return os.path.join(self.prefix, self.name, self.meta_file_name)

    def getInfo(self):
        return self.meta

    def read_meta_file(self):
        try:
            s = self.load_file(self.get_meta_path())
            return json.loads(s)
        except IOError:
            meta = {
                'name': self.name,
                'files': []
            }
            self.sync_meta(meta)
            return meta

    def sync_meta(self, content):
        self.write_file(self.get_meta_path(), json.dumps(content, sort_keys=True, indent=4, separators=(',', ': ')))

    def restore(self, path, content):
        # if content is not null, save it
        # return load(path)
        pass

    def _save(self, path, content):
        drive, basename, filename = self.pathParse(path)
        ret = self.make_sure_path_exists(basename)
        if ret != True:
            return False # TODO: should return a specify value

        self.write_file(path, content)
        return True

    def save(self, content):
        path = self.calendarToPath(self.calendar)
        if self._save(path, content) == True:
            self.meta['files'].append({'path': path, 'modified_time': str(self.calendar.now())})
            self.sync_meta(self.meta)

    def test(self):
        return self.calendar.now()

    @staticmethod
    def make_sure_path_exists(path):
        try:
            os.makedirs(path)
        except OSError as exception:
            if exception.errno == errno.EEXIST:
                return True
            else:
                return False
        else:
            return True

    @staticmethod
    def pathParse(path):
        drive, path_and_file = os.path.splitdrive(path)
        basename, filename = os.path.split(path_and_file)
        return [drive, basename, filename]

    @staticmethod
    def load_file(path):
        file = open(path, 'r')
        content = file.read()
        file.close()
        return content

    @staticmethod
    def write_file(path, content):
        drive, basename, filename = Backup.pathParse(path)
        ret = Backup.make_sure_path_exists(basename)
        if ret != True:
            return False # TODO: should return a specify value

        file = open(path, 'w')
        file.write(content)
        file.close()

class BackupTest(unittest.TestCase):

    def setUp(self):
        self.cleanUp()

    def tearDown(self):
        self.cleanUp()

    def cleanUp(self):
        shutil.rmtree(Backup.prefix, ignore_errors=True)

    def test_abc(self):
        self.assertEqual(True, True)

    def load_file(self, path):
        file = open(path, 'r')
        content = file.read()
        file.close()
        return content

    def test_make_sure_path_exists(self):
        path = os.path.join(Backup.prefix, 'foo')
        ret = Backup.make_sure_path_exists(path)
        self.assertEqual(ret, True)
        self.assertEqual(os.path.exists(path), True)
        self.assertEqual(os.path.isdir(path), True)
        ret = Backup.make_sure_path_exists(path)
        self.assertEqual(ret, True)

    def test_pathParse(self):
        drive, basename, filename = Backup.pathParse(os.path.join('test', 'foo', 'foo-yyyy-mm-ddThh-mm-ss.json'))
        self.assertEqual(drive, '')
        self.assertEqual(basename, os.path.join('test', 'foo'))
        self.assertEqual(filename, 'foo-yyyy-mm-ddThh-mm-ss.json')

    def test_calendarToPath(self):
        from datetime import datetime
        calendar = datetime(2010, month=1, day=1)
        backup = Backup('test', calendar)
        backup.init()
        path = backup.calendarToPath(calendar)
        self.assertEqual(path, os.path.join(backup.prefix, 'test/test-2010-01-01T00-00-00.json'))

    def test_save_file(self):
        from datetime import datetime
        calendar = datetime(2010, month=1, day=1)
        backup = Backup('test', calendar)
        backup.init()
        path = backup.calendarToPath(calendar)

        content = json.dumps({"c": 0, "b": 0, "a": 0})
        backup._save(path, content)
        actual = self.load_file(path)
        self.assertEqual(actual, content)

    def test_save_config(self):
        from datetime import datetime
        calendar = datetime(2010, month=1, day=1)
        backup = Backup('test', calendar)
        backup.init()
        content = json.dumps({"c": 1, "b": 1, "a": 1})
        backup.save(content)

        # should use getInfo to get the last cfg file
        info = backup.getInfo()
        self.assertEqual(len(info['files']), 1)
        actual = self.load_file('./cfg/test/test-2010-01-01T00-00-00.json')
        self.assertEqual(actual, content)

    def test_save_two_configs(self):
        inputs = [{'content': '{a:1, b:1, c:1}'},
                  {'content': '{a:2, b:2, c:2}'}]

        from datetime import datetime
        calendar = datetime(2010, month=1, day=1)
        backup = Backup('test', calendar)
        backup.init()

        for input in inputs:
            backup.save(input['content'])

        info = backup.getInfo()
        self.assertEqual(len(info['files']), len(inputs))

if __name__ == '__main__':
    unittest.main()