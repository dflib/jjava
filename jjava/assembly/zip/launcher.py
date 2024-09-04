import os
import subprocess
import sys


def launch_kernel():
    kernel_path = sys.argv[1]
    connection_file = sys.argv[2]

    args = ['java', '--add-opens', 'jdk.jshell/jdk.jshell=ALL-UNNAMED', '-jar', kernel_path, connection_file]

    jvm_options = os.getenv('JJAVA_JVM_OPTS', '')
    if jvm_options:
        args[1:1] = jvm_options.split(' ')

    print(f"Running JJava Kernel with args: {args}")
    subprocess.run(args)


if __name__ == '__main__':
    launch_kernel()
