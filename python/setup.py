from distutils.core import setup, Extension

setup(name='abstract_rendering', 
      version='0.0.1',
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
      ext_modules=[Extension('abstract_rendering.transform',
                             ['transform.cpp'],
                             extra_compile_args=['-std=c++11','-O3', '-Wall', '-march=native', '-fno-rtti', '-fno-exceptions', '-fPIC', '-lstdc++']),
                   Extension('abstract_rendering.transform_libdispatch',
                             ['transform_libdispatch.cpp'], 
                             extra_compile_args=['-std=c++11','-O3', '-Wall', '-march=native', '-fno-rtti', '-fno-exceptions', '-fPIC', '-lstdc++'])]
     )
      
