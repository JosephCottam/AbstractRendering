import unittest
import abstract_rendering.general as general
import numpy as np

class SpreadTests(unittest.TestCase):
  def run_spread(self, spread, in_vals, expected):
    spread = general.Spread(spread)
    out = spread.shade(in_vals)
    
    self.assertTrue(np.array_equal(out, expected), 'incorrect value spreading')

  def test_spread_oneseed(self):
    a = np.asarray([[0,0,0,0],
                    [0,1,0,0],
                    [0,0,0,0],
                    [0,0,0,0]])
    
    ex = np.asarray([[0,0,0,0],
                     [0,1,1,0],
                     [0,1,1,0],
                     [0,0,0,0]])
    self.run_spread(2, a, ex)

  def test_spread_twoseeds(self):
    a = np.asarray([[0,0,0,0],
                    [0,1,1,0],
                    [0,0,0,0],
                    [0,0,0,0]])
    
    ex = np.asarray([[0,0,0,0],
                     [0,1,2,1],
                     [0,1,2,1],
                     [0,0,0,0]])
    self.run_spread(2, a, ex)


  def test_spread_three(self):
    a = np.asarray([[0,0,0,0],
                    [0,1,0,0],
                    [0,0,0,0],
                    [0,0,0,0]])
    
    ex = np.asarray([[1,1,1,0],
                     [1,1,1,0],
                     [1,1,1,0],
                     [0,0,0,0]])
    self.run_spread(3, a, ex)

  def spread_zero(self):
    a = np.asarray([[0,0,0,0],
                    [0,1,0,0],
                    [0,0,0,0],
                    [0,0,0,0]])
    
    ex = np.asarray([[0,0,0,0],
                     [0,1,0,0],
                     [0,0,0,0],
                     [0,0,0,0]])
    self.run_spread(0, a, ex)


if __name__ == '__main__':
    unittest.main()
