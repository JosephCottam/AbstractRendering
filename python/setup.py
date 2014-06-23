from distutils.core import setup, Extension
import sys

### Flag to indicate that libdispatch should be used (OS X only)
DISPATCH_FLAG = "--dispatch"

if DISPATCH_FLAG in sys.argv:
  transform = Extension('abstract_rendering.transform_libdispatch',
                          ['transform_libdispatch.cpp'], 
                           extra_compile_args=['-std=c++11','-O3', '-Wall', '-march=native', '-fno-rtti', '-fno-exceptions', '-fPIC', '-lstdc++'])
  del sys.argv[sys.argv.index(DISPATCH_FLAG)]

else:
  transform = Extension('abstract_rendering.transform',
                             ['transform.cpp'],
                             extra_compile_args=['-std=c++11','-O3', '-Wall', '-march=native', '-fno-rtti', '-fno-exceptions', '-fPIC', '-lstdc++'])



setup(name='abstract_rendering', 
      version='0.0.2',
      description='Rendering as a binning process',
      author='Joseph Cottam',
      author_email='jcottam@indiana.edu',
      url='https://github.com/JosephCottam/AbstractRendering',
      package_dir = {'abstract_rendering' : '.'},
      py_modules=['abstract_rendering.core', 
                  'abstract_rendering.general', 
                  'abstract_rendering.categories',
                  'abstract_rendering.fast_project',
                  'abstract_rendering.geometry',
                  'abstract_rendering.glyphset', 
                  'abstract_rendering.infos',
                  'abstract_rendering.numeric'],
      ext_modules=[transform],
      classifiers=["Development Status :: 3 - Alpha", "Intended Audience :: Science/Research", "Topic :: Scientific/Engineering :: Visualization"]

      )
