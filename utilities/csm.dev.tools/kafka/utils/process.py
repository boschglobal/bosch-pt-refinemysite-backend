import subprocess
import sys
import textwrap


class ProcessUtils:
    @staticmethod
    def run(operation, executable_path='', working_directory=''):
        """Run an executable located at the specified directory 
        in a sub-process and returns the std-out and std-err"""

        if working_directory == '':
            working_directory = sys.path[0]

        command = executable_path + operation
        p = subprocess.Popen(command, cwd=working_directory, shell=True,
                             stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE)

        out, err = p.communicate()
        output = out.decode('utf-8').strip(' \t\n\r')
        error = err.decode('utf-8').strip(' \t\n\r')
        return output, error

    @staticmethod
    def print(output, error):
        """Print std-out and std-err on the command line"""
        wrapper = textwrap.TextWrapper(
            initial_indent='   ', subsequent_indent='   ')
        if output != '':
            print(wrapper.fill(output))
        if error != '':
            print(wrapper.fill(error))
