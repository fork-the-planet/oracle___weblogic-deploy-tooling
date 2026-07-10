"""
Copyright (c) 2026, Oracle and/or its affiliates.
Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
"""
import os

from oracle.weblogic.deploy.util import WLSDeployArchive

from base_test import BaseTestCase
from wlsdeploy.aliases import model_constants
from wlsdeploy.tool.discover.jms_resources_discoverer import JmsResourcesDiscoverer


class _ModelContext(object):

    def __init__(self, archive_file, domain_home):
        self._archive_file = archive_file
        self._domain_home = domain_home

    def get_archive_file(self):
        return self._archive_file

    def get_domain_home(self):
        return self._domain_home

    def is_remote(self):
        return False

    def is_skip_archive(self):
        return False

    def is_ssh(self):
        return False

    def replace_token_string(self, value):
        return value

    def tokenize_path(self, value):
        return str(value).replace(str(self._domain_home), '@@DOMAIN_HOME@@')


class JmsResourcesDiscovererTest(BaseTestCase):

    def __init__(self, *args):
        BaseTestCase.__init__(self, *args)

    def setUp(self):
        BaseTestCase.setUp(self)

    def tearDown(self):
        BaseTestCase.tearDown(self)

    def test_add_foreign_server_binding_directory(self):
        archive_path = os.path.join(self.TEST_OUTPUT_DIR, 'discover-foreign-server-directory.zip')
        if os.path.exists(archive_path):
            os.remove(archive_path)

        archive_file = WLSDeployArchive(archive_path)
        try:
            source_directory = os.path.join(self.TEST_CLASSES_DIR, 'my-app')
            model_context = _ModelContext(archive_file, os.getcwd())
            resources_discoverer = JmsResourcesDiscoverer.__new__(JmsResourcesDiscoverer)
            resources_discoverer._model_context = model_context
            resources_discoverer.path_helper = self.path_helper

            result = resources_discoverer._add_foreign_server_binding(
                'fs1', model_constants.CONNECTION_URL, 'file://' + source_directory)

            expected = 'file:///@@DOMAIN_HOME@@/' + WLSDeployArchive.ARCHIVE_JMS_FOREIGN_SERVER_DIR + \
                '/fs1/my-app/'
            self.assertEqual(expected, result)
            archive_directory = WLSDeployArchive.ARCHIVE_JMS_FOREIGN_SERVER_DIR + '/fs1/my-app/'
            self.assertEquals(True, archive_file.containsPath(archive_directory))
        finally:
            archive_file.close()


if __name__ == '__main__':
    import unittest
    unittest.main()
